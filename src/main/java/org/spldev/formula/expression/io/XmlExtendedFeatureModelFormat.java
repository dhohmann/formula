/* -----------------------------------------------------------------------------
 * Formula Lib - Library to represent and edit propositional formulas.
 * Copyright (C) 2021  Sebastian Krieter
 *
 * This file is part of Formula Lib.
 *
 * Formula Lib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Formula Lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Formula Lib.  If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/skrieter/formula> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.formula.expression.io;

import org.spldev.formula.expression.Formula;
import org.spldev.formula.expression.Formulas;
import org.spldev.formula.expression.atomic.literal.Literal;
import org.spldev.formula.expression.atomic.literal.LiteralPredicate;
import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.atomic.predicate.Equals;
import org.spldev.formula.expression.compound.And;
import org.spldev.formula.expression.compound.Implies;
import org.spldev.formula.expression.compound.Not;
import org.spldev.formula.expression.compound.Or;
import org.spldev.formula.expression.term.Term;
import org.spldev.formula.expression.term.Variable;
import org.spldev.formula.expression.term.attribute.AggregationType;
import org.spldev.formula.expression.term.bool.BoolVariable;
import org.spldev.formula.expression.term.integer.IntAdd;
import org.spldev.formula.expression.term.integer.IntConstant;
import org.spldev.formula.expression.term.integer.IntVariable;
import org.spldev.formula.expression.term.real.RealConstant;
import org.spldev.formula.expression.term.real.RealVariable;
import org.spldev.formula.expression.transform.NormalForms;
import org.spldev.util.data.Result;
import org.spldev.util.io.format.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.*;

/**
 * Format class for the legacy xml based format from FeatureIDE. Provides
 * support for extended and normal feature models.
 */
public class XmlExtendedFeatureModelFormat extends XmlFeatureModelFormat {

	public static final String ID = XmlExtendedFeatureModelFormat.class.getCanonicalName();

	protected final static String EXTENDED_FEATURE_MODEL = "extendedFeatureModel";
	protected final static String ATTRIBUTE = "attribute";

	protected final static String COUNT_SUFFIX = ".count";

	private final HashMap<String, Object> attributeValues; // TODO: Eigene Klasse für Features, Attr,
	// werte

	public XmlExtendedFeatureModelFormat() {
		this(new HashMap());
	}

	public XmlExtendedFeatureModelFormat(HashMap<String, Object> attributes) {
		super();
		attributeValues = attributes;
	}

	/**
	 * Searches in element for names with the name provided in nodeNames. The order
	 * of names is respected.
	 *
	 * @param document  Document to search in
	 * @param nodeNames Names to search for
	 * @return List of found elements that matched a nodeName
	 */
	protected List<Element> getElement(Document document, String... nodeNames) {
		for (String nodeName : nodeNames) {
			final List<Element> elements = getElement(document, nodeName);
			if (!elements.isEmpty()) {
				return elements;
			}
		}
		return Collections.emptyList();
	}

	protected Formula readDocument(Document doc) throws ParseException {
		map = VariableMap.emptyMap();
		final List<Element> elementList = getElement(doc, EXTENDED_FEATURE_MODEL, FEATURE_MODEL);
		if (elementList.size() == 1) {
			final Element e = elementList.get(0);
			parseStruct(getElement(e, STRUCT));
			parseConstraints(getElement(e, CONSTRAINTS));
		} else if (elementList.isEmpty()) {
			throw new ParseException("Not a feature model xml element!");
		} else {
			throw new ParseException("More than one feature model xml elements!");
		}
		Formula modelFormula;
		if (constraints.isEmpty()) {
			modelFormula = And.empty(map);
		} else {
			if (constraints.get(0).getChildren().isEmpty()) {
				constraints.set(0, Or.empty(map));
			}
			modelFormula = new And(constraints);
		}

		// Uses Tseytin-Transformation
		Result<Formula> cnfModel = Formulas.toCNF(NormalForms.simplifyForNF(modelFormula));
		if (cnfModel.isPresent()) {
			modelFormula = cnfModel.get();
		}
		List<Formula> extendedConstraints = new ArrayList<>();
		extendedConstraints.addAll(buildCount());
		extendedConstraints.addAll(buildAttributeAssignments());

		if (extendedConstraints.isEmpty()) {
			return modelFormula;
		}

		Formula formulaExtension = new And(extendedConstraints);
		Formula result = new And(formulaExtension, modelFormula);
		return result;
	}

	/**
	 * Builds a counter for selected features. It utilizes variables ending with
	 * {@value #COUNT_SUFFIX}. The result of the counter can be addressed by using
	 * the variable <code>count</code>.
	 *
	 * @return formula to add to the general constraints
	 */
	private List<Formula> buildCount() {
		// TODO Move to correct class
		List<Formula> constraints = new ArrayList<>();
		Term<Long> l = null;
		List<String> featureNames = map.getNames();
		for (String featureName : featureNames) {
			BoolVariable feature = (BoolVariable) map.getVariable(featureName).get();
			IntVariable var = map.addIntegerVariable(featureName + COUNT_SUFFIX).orElseThrow(RuntimeException::new);
			LiteralPredicate selected = new LiteralPredicate(feature, true);
			constraints.add(new Implies(selected, new Equals<>(var, new IntConstant(1L))));
			constraints.add(new Implies(new Not(selected), new Equals<>(var, new IntConstant(0L))));
			if (l == null) {
				l = var;
			} else {
				l = new IntAdd(l, var);
			}
		}
		IntVariable count = map.addIntegerVariable("count").get();
		Equals<Long> assignment = new Equals<>(count, l);
		constraints.add(assignment);
		return constraints;
	}

	/**
	 * Retrieves a map of all features and feature attributes.
	 *
	 * @return variable map
	 */
	public VariableMap getAllSmtVariables() {
		VariableMap complete = VariableMap.emptyMap();
		for (int i = 1; i <= map.size(); i++) {
			Variable<?> v = map.getVariable(i).get();
			if (v instanceof IntVariable) {
				complete.addIntegerVariable(v.getName());
			} else if (v instanceof BoolVariable) {
				complete.addBooleanVariable(v.getName());
			} else if (v instanceof RealVariable) {
				complete.addRealVariable(v.getName());
			}
		}
		for (String name : attributeValues.keySet()) {
			Object value = attributeValues.get(name);
			if (value instanceof Double) {
				complete.addRealVariable(name);
			} else if (value instanceof Integer) {
				complete.addIntegerVariable(name);
			}
		}
		return complete;
	}

	@Override
	protected LiteralPredicate parseFeature(Literal parent, final Element e, final String nodeName, boolean and)
		throws ParseException {

		boolean mandatory = false;
		String name = null;
		if (e.hasAttributes()) {
			final NamedNodeMap nodeMap = e.getAttributes();
			for (int i = 0; i < nodeMap.getLength(); i++) {
				final org.w3c.dom.Node node = nodeMap.item(i);
				final String attributeName = node.getNodeName();
				final String attributeValue = node.getNodeValue();
				if (attributeName.equals(MANDATORY)) {
					mandatory = attributeValue.equals(TRUE);
				} else if (attributeName.equals(NAME)) {
					name = attributeValue;
				}
			}
		}
		if (map.getIndex(name).isEmpty()) {
			map.addBooleanVariable(name);
		} else {
			throw new ParseException("Duplicate feature name!");
		}

		final LiteralPredicate f = new LiteralPredicate((BoolVariable) map.getVariable(name).get(), true);

		if (parent == null) {
			constraints.add(f);
		} else {
			constraints.add(implies(f, parent));
			if (and && mandatory) {
				constraints.add(implies(parent, f));
			}
		}

		if (e.hasChildNodes()) {
			final ArrayList<Formula> parseFeatures = parseFeatures(e.getChildNodes(), f, nodeName.equals(AND));
			switch (nodeName) {
			case AND:
				break;
			case OR:
				constraints.add(implies(f, parseFeatures));
				break;
			case ALT:
				if (parseFeatures.size() == 1) {
					constraints.add(implies(f, parseFeatures.get(0)));
				} else {
					constraints.add(new And(implies(f, parseFeatures), atMost(parseFeatures)));
				}
				break;
			default:
				break;
			}
		} else if (!"feature".equals(nodeName)) {
			throw new ParseException("Empty group!");
		}

		if (e.hasChildNodes()) {
			List<Element> children = getElements(e.getChildNodes());
			for (Element child : children) {
				if (ATTRIBUTE.equals(child.getNodeName())) {
					parseAttribute(name, child);
				}
			}
		} else if (!"feature".equals(nodeName)) {
			throw new ParseException("Empty group!");
		}

		return f;
	}

	/**
	 * Parses an attribute node in the xml structure.
	 *
	 * @param parent Feature name the attribute belongs to
	 * @param e      Element with the attribute information
	 */
	protected void parseAttribute(String parent, Element e) {
		if (!e.hasAttribute("type") || !e.hasAttribute("value")) {
			return;
		}
		String name = e.getAttribute(NAME) + "(" + parent + ")";
		Object value = null;
		switch (e.getAttribute("type")) {
		case "long":
			value = Long.parseLong(e.getAttribute("value"));
			break;
		case "double":
			value = Double.parseDouble(e.getAttribute("value"));
			break;
		default:
			break;
		}
		if (value != null) {
			attributeValues.put(name, value);
		}
	}

	/**
	 * Creates an alternative selection using XOR.
	 *
	 * @param parseFeatures All features available for selection
	 * @return parent implies (((f1 XOR f2) XOR ...) XOR fn)
	 */
	protected Formula alternative(ArrayList<Formula> parseFeatures) {
		List<Formula> options = new ArrayList<>();
		for (int i = 0; i < parseFeatures.size(); i++) {
			List<Formula> p = new ArrayList<>();
			for (int j = 0; j < parseFeatures.size(); j++) {
				if (i == j) {
					p.add(parseFeatures.get(j));
				} else {
					p.add(new Not(parseFeatures.get(j)));
				}
			}
			options.add(new And(p));
		}
		return new Or(options);
	}

	/**
	 * Creates the attribute value assignments for all supported aggregations.
	 *
	 * @return List containing all assignments
	 * @see AggregationType
	 */
	protected List<Formula> buildAttributeAssignments() throws ParseException {
		List<Formula> assignments = new ArrayList<>();

		for (String variable : attributeValues.keySet()) {
			String featureName = variable.substring(variable.indexOf('(') + 1, variable.indexOf(')'));
			Object value = attributeValues.get(variable);

			// Include values from feature model
			Variable<?> attributeValueFromModel = null;
			if (value instanceof Long) {
				attributeValueFromModel = map.addIntegerVariable(variable).orElseThrow(() -> new RuntimeException(
					"Could not create variable" + variable));
				assignments.add(new Equals<>((IntVariable) attributeValueFromModel, new IntConstant((Long) value)));
			} else if (value instanceof Double) {
				attributeValueFromModel = map.addRealVariable(variable).orElseThrow(() -> new RuntimeException(
					"Could not create variable" + variable));
				assignments.add(new Equals<>((RealVariable) attributeValueFromModel, new RealConstant((Double) value)));
			}

			if (attributeValueFromModel == null) {
				throw new ParseException("Could not create variable " + variable);
			}

			// Build assignment logic for selection process
			Optional<Variable<?>> boolVariable = map.getVariable(featureName);
			if (!boolVariable.isPresent()) {
				throw new ParseException("Feature " + featureName + " could not be found");
			}
			BoolVariable feature = (BoolVariable) boolVariable.get();
			LiteralPredicate selected = new LiteralPredicate(feature, true);
			for (AggregationType agg : AggregationType.values()) {
				String aggVarName = variable + "." + agg.name();
				if (value instanceof Long) {
					IntConstant defaultValue = new IntConstant(agg.getDefaultValue());
					IntVariable aggVar = map.addIntegerVariable(aggVarName).get();
					assignments.add(new Implies(selected, new Equals<>(aggVar, (IntVariable) attributeValueFromModel)));
					assignments.add(new Implies(new Not(selected), new Equals<>(aggVar, defaultValue)));
				} else if (value instanceof Double) {
					RealConstant defaultValue = new RealConstant(agg.getDefaultValue().doubleValue());
					RealVariable aggVar = map.addRealVariable(aggVarName).get();
					assignments.add(new Implies(selected, new Equals<>(aggVar,
						(RealVariable) attributeValueFromModel)));
					assignments.add(new Implies(new Not(selected), new Equals<>(aggVar, defaultValue)));
				}
			}
		}
		return assignments;
	}

	/**
	 * Returns the attribute values extracted from the feature model.
	 *
	 * @return Variable assignments
	 */
	public HashMap<String, Object> getAttributeValues() {
		return attributeValues;
	}

	@Override
	public XmlExtendedFeatureModelFormat getInstance() {
		return this;
	}

	@Override
	public String getIdentifier() {
		return ID;
	}

	@Override
	public String getName() {
		return "FeatureIDE-Extended";
	}

}
