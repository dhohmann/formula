package org.spldev.formula.expression.io;

import org.spldev.formula.expression.Formula;
import org.spldev.formula.expression.atomic.literal.ErrorLiteral;
import org.spldev.formula.expression.atomic.literal.Literal;
import org.spldev.formula.expression.atomic.literal.LiteralPredicate;
import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.atomic.predicate.*;
import org.spldev.formula.expression.compound.*;
import org.spldev.formula.expression.term.Constant;
import org.spldev.formula.expression.term.Term;
import org.spldev.formula.expression.term.Variable;
import org.spldev.formula.expression.term.attribute.*;
import org.spldev.formula.expression.term.bool.BoolVariable;
import org.spldev.formula.expression.term.integer.IntConstant;
import org.spldev.formula.expression.term.integer.IntMultiply;
import org.spldev.formula.expression.term.integer.attribute.IntAverage;
import org.spldev.formula.expression.term.integer.attribute.IntProduct;
import org.spldev.formula.expression.term.integer.attribute.IntSum;
import org.spldev.formula.expression.term.real.RealConstant;
import org.spldev.formula.expression.term.real.attribute.RealAverage;
import org.spldev.formula.expression.term.real.attribute.RealProduct;
import org.spldev.formula.expression.term.real.attribute.RealSum;
import org.spldev.util.data.Problem;
import org.spldev.util.data.Result;
import org.spldev.util.io.PositionalXMLHandler;
import org.spldev.util.io.format.Format;
import org.spldev.util.io.format.Input;
import org.spldev.util.io.format.ParseException;
import org.spldev.util.io.format.ParseProblem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.spldev.formula.expression.io.XmlFeatureModelFormat.*;

public class ConfiguringConstraintsFormat implements Format<Formula> {

	public static final String SUM = "sum";
	public static final String MUL = "mul";
	public static final String AVG = "avg";
	public static final String EQUALS = "equals";
	public static final String LESS_THAN = "lessThan";
	public static final String LESS_EQUALS = "lessEquals";
	public static final String GREATER_THAN = "greaterThan";
	public static final String GREATER_EQUALS = "greaterEquals";

	private VariableMap map;
	private List<Formula> constraints = new ArrayList<>();
	private List<Problem> parseProblems = new ArrayList<>();

	public ConfiguringConstraintsFormat(VariableMap variables) {
		map = variables;
	}

	@Override
	public String getFileExtension() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Result<Formula> parse(Input source) {
		try {
			parseProblems.clear();
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(source.getReader()),
				new PositionalXMLHandler(doc));
			doc.getDocumentElement().normalize();
			return Result.of(readDocument(doc), parseProblems);
		} catch (final Exception e) {
			return Result.empty(new Problem(e));
		}
	}

	protected Formula readDocument(Document doc) throws ParseException {
		final List<Element> elementList = getElement(doc, CONSTRAINTS);
		if (elementList.size() == 1) {
			parseConstraints(elementList);
		} else if (elementList.isEmpty()) {
			throw new ParseException("Not a constraint xml element!");
		} else {
			throw new ParseException("More than one constraint xml elements!");
		}
		if (constraints.isEmpty()) {
			return And.empty();
		} else {
			if (constraints.get(0).getChildren().isEmpty()) {
				constraints.set(0, Or.empty(map));
			}
		}
		return new And(constraints);
	}

	protected List<Element> getElement(final Element element, final String nodeName) {
		return getElements(element.getElementsByTagName(nodeName));
	}

	protected List<Element> getElement(final Document document, final String nodeName) {
		return getElements(document.getElementsByTagName(nodeName));
	}

	/**
	 * Returns a list of elements within the given node list.
	 *
	 * @param nodeList the node list.
	 * @return The child nodes from type Element of the given NodeList.
	 */
	protected static final List<Element> getElements(NodeList nodeList) {
		final ArrayList<Element> elements = new ArrayList<>(nodeList.getLength());
		for (int temp = 0; temp < nodeList.getLength(); temp++) {
			final org.w3c.dom.Node nNode = nodeList.item(temp);
			if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				final Element eElement = (Element) nNode;
				elements.add(eElement);
			}
		}
		return elements;
	}

	protected void parseConstraints(List<Element> elements) throws ParseException {
		if (elements.size() > 1) {
			throw new ParseException("Multiple <constraints> elements!");
		}
		for (final Element e : elements) {
			for (final Element child : getElements(e.getChildNodes())) {
				final String nodeName = child.getNodeName();
				if (nodeName.equals(RULE)) {
					try {
						final List<Formula> parseConstraintNode = parseConstraintNode(child.getChildNodes());
						if (parseConstraintNode.size() == 1) {
							constraints.add(parseConstraintNode.get(0));
						} else {
							parseProblems.add(new ParseProblem(nodeName,
								(int) child.getUserData(PositionalXMLHandler.LINE_NUMBER_KEY_NAME),
								Problem.Severity.WARNING));
						}
					} catch (final Exception exception) {
						parseProblems.add(new ParseProblem(exception.getMessage(),
							(int) child.getUserData(PositionalXMLHandler.LINE_NUMBER_KEY_NAME),
							Problem.Severity.WARNING));
					}
				}
			}
		}
	}

	protected List<Formula> parseConstraintNode(NodeList nodeList) throws ParseException {
		final List<Formula> nodes = new ArrayList<>();
		List<Formula> children;
		final List<Element> elements = getElements(nodeList);
		for (final Element e : elements) {
			final String nodeName = e.getNodeName();
			switch (nodeName) {
			case DISJ:
				children = parseConstraintNode(e.getChildNodes());
				if (!children.isEmpty()) {
					nodes.add(new Or(children));
				}
				break;
			case CONJ:
				children = parseConstraintNode(e.getChildNodes());
				if (!children.isEmpty()) {
					nodes.add(new And(children));
				}
				break;
			case EQ:
				children = parseConstraintNode(e.getChildNodes());
				if (children.size() == 2) {
					nodes.add(biimplies(children.get(0), children.get(1)));
				}
				break;
			case IMP:
				children = parseConstraintNode(e.getChildNodes());
				nodes.add(implies(children.get(0), children.get(1)));
				break;
			case NOT:
				children = parseConstraintNode(e.getChildNodes());
				if (children.size() == 1) {
					nodes.add(new Not(children.get(0)));
				}
				break;
			case ATMOST1:
				children = parseConstraintNode(e.getChildNodes());
				if (!children.isEmpty()) {
					nodes.add(atMost(children));
				}
				break;
			case VAR:
				nodes.add(map.getVariable(e.getTextContent())
					.map(v -> (Literal) new LiteralPredicate((BoolVariable) v, true))
					.orElse(new ErrorLiteral(nodeName)));
				break;
			case EQUALS:
				nodes.add(createEquals(e));
				break;
			case LESS_THAN:
				nodes.add(createLessThan(e));
				break;
			case LESS_EQUALS:
				nodes.add(createLessEquals(e));
				break;
			case GREATER_THAN:
				nodes.add(createGreaterThan(e));
				break;
			case GREATER_EQUALS:
				nodes.add(createGreaterEquals(e));
				break;
			default:
				throw new ParseException(nodeName);
			}
		}
		return nodes;
	}

	private Formula createGreaterThan(Element e) throws ParseException {
		final List<Element> elements = getElements(e.getChildNodes());
		final List<Term<?>> terms = parseTerms(elements);
		if (terms.size() != 2) {
			throw new ParseException("Function compares two items");
		}
		return new GreaterThan(terms.get(0), terms.get(1));
	}

	private Formula createGreaterEquals(Element e) throws ParseException {
		final List<Element> elements = getElements(e.getChildNodes());
		final List<Term<?>> terms = parseTerms(elements);
		if (terms.size() != 2) {
			throw new ParseException("Function compares two items");
		}
		return new GreaterEqual(terms.get(0), terms.get(1));
	}

	private Formula createLessEquals(Element e) throws ParseException {
		final List<Element> elements = getElements(e.getChildNodes());
		final List<Term<?>> terms = parseTerms(elements);
		if (terms.size() != 2) {
			throw new ParseException("Function compares two items");
		}
		return new LessEqual(terms.get(0), terms.get(1));
	}

	private Formula createLessThan(Element e) throws ParseException {
		final List<Element> elements = getElements(e.getChildNodes());
		final List<Term<?>> terms = parseTerms(elements);
		if (terms.size() != 2) {
			throw new ParseException("Function compares two items");
		}
		return new LessThan(terms.get(0), terms.get(1));
	}

	private Formula createEquals(Element node) throws ParseException {
		final List<Element> elements = getElements(node.getChildNodes());
		final List<Term<?>> terms = parseTerms(elements);
		if (terms.size() != 2) {
			throw new ParseException("Function compares two items");
		}
		return new Equals(terms.get(0), terms.get(1));
	}

	private List<Term<?>> parseTerms(List<Element> elements) throws ParseException {
		final List<Term<?>> terms = new ArrayList<>();
		for (Element e : elements) {
			terms.add(parseTerm(e));
		}
		return terms;
	}

	private Term<?> parseTerm(Element element) throws ParseException {
		String nodeName = element.getNodeName();
		switch (nodeName) {
		case SUM:
		case MUL:
		case AVG:
			return createAggregation(element);
		case VAR:
			return map.getVariable(element.getTextContent()).get();
		case "const":
			return createConstant(element);
		default:
			throw new ParseException("Could not parse term from " + element.toString());
		}
	}

	private Aggregation<?> createAggregation(Element e) throws ParseException {
		String type = e.getAttribute("type");
		String aggregation = e.getNodeName();
		AggregationType aggType = AggregationType.valueOf(aggregation.toUpperCase());
		switch (aggType) {
		case AVG:
			return createAvg(e, type);
		case MUL:
			return createMul(e, type);
		case SUM:
			return createSum(e, type);
		default:
			throw new ParseException("Could not create aggregation " + aggregation);
		}
	}

	private Sum<?> createSum(Element e, String type) throws ParseException {
		switch (type) {
		case "long":
			return new IntSum(e.getTextContent(), map);
		case "double":
			return new RealSum(e.getTextContent(), map);
		default:
			throw new ParseException("Sum of type " + type + " is not supported");
		}
	}

	private Product<?> createMul(Element e, String type) throws ParseException {
		switch (type) {
		case "long":
			return new IntProduct(e.getTextContent(), map);
		case "double":
			return new RealProduct(e.getTextContent(), map);
		default:
			throw new ParseException("Sum of type " + type + " is not supported");
		}
	}

	private Average<?> createAvg(Element e, String type) throws ParseException {
		switch (type) {
		case "long":
			return new IntAverage(e.getTextContent(), map);
		case "double":
			return new RealAverage(e.getTextContent(), map);
		default:
			throw new ParseException("Sum of type " + type + " is not supported");
		}
	}

	private Constant<?> createConstant(Element e) throws ParseException {
		String type = e.getAttribute("type");
		switch (type) {
		case "long":
			return new IntConstant(Long.valueOf(e.getTextContent()));
		case "double":
			return new RealConstant(Double.valueOf(e.getTextContent()));
		default:
			throw new ParseException("Constant type " + type + " is not supported");
		}
	}

	protected Formula atMost(final List<Formula> parseFeatures) {
		return new AtMost(parseFeatures, 1);
	}

	protected Formula biimplies(Formula a, final Formula b) {
		return new Biimplies(a, b);
	}

	protected Formula implies(Literal a, final Formula b) {
		return new Implies(a, b);
	}

	protected Formula implies(Formula a, final Formula b) {
		return new Implies(a, b);
	}

	protected Formula implies(final LiteralPredicate f, final List<Formula> parseFeatures) {
		return parseFeatures.size() == 1
			? new Implies(f, parseFeatures.get(0))
			: new Implies(f, new Or(parseFeatures));
	}

	@Override
	public Format<Formula> getInstance() {
		return this;
	}

	@Override
	public boolean supportsParse() {
		return true;
	}
}
