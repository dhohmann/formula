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
import org.spldev.formula.expression.atomic.literal.Literal;
import org.spldev.formula.expression.atomic.literal.LiteralPredicate;
import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.atomic.predicate.Equals;
import org.spldev.formula.expression.compound.*;
import org.spldev.formula.expression.term.Term;
import org.spldev.formula.expression.term.Variable;
import org.spldev.formula.expression.term.attribute.AggregationType;
import org.spldev.formula.expression.term.bool.BoolVariable;
import org.spldev.formula.expression.term.integer.IntAdd;
import org.spldev.formula.expression.term.integer.IntConstant;
import org.spldev.formula.expression.term.integer.IntVariable;
import org.spldev.formula.expression.term.real.RealConstant;
import org.spldev.formula.expression.term.real.RealVariable;
import org.spldev.util.io.format.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.*;

/**
 * Format class for the legacy xml based format from FeatureIDE. Provides support for extended and normal feature models.
 */
public class XmlExtendedFeatureModelFormat extends XmlFeatureModelFormat {

    public static final String ID = XmlExtendedFeatureModelFormat.class.getCanonicalName();

    protected final static String EXTENDED_FEATURE_MODEL = "extendedFeatureModel";
    protected final static String ATTRIBUTE = "attribute";

    protected final static String COUNT_SUFFIX = ".count";

    private final HashMap<String, Object> attributeValues = new HashMap(); // TODO: Eigene Klasse für Features, Attr, werte

    public XmlExtendedFeatureModelFormat() {
        super();
    }


    /**
     * Searches in element for names with the name provided in nodeNames. The order of names is respected.
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

        // TODO Geht mit SAT-Solvern nicht
        if (!constraints.isEmpty()) {
            constraints.add(buildCount());
        }

        if (!attributeValues.isEmpty()) {
            constraints.add(buildAssignments());
        }

        if (constraints.isEmpty()) {
            return And.empty(map);
        } else {
            if (constraints.get(0).getChildren().isEmpty()) {
                constraints.set(0, Or.empty(map));
            }
        }
        return new And(constraints);
    }

    /**
     * Builds a counter for selected features. It utilizes variables ending with {@value #COUNT_SUFFIX}.
     * The result of the counter can be addressed by using the variable <code>count</code>.
     *
     * @return formula to add to the general constraints
     */
    private Formula buildCount() {
        // TODO Move to correct class
        List<Formula> constraints = new ArrayList<>();
        Term<Long> l = null;
        for (String name : map.getNames()) {
            if (name.endsWith(COUNT_SUFFIX)) {
                BoolVariable feature = (BoolVariable) map.getVariable(name.substring(0, name.indexOf(COUNT_SUFFIX))).get();
                IntVariable var = (IntVariable) map.getVariable(name).get();
                LiteralPredicate selected = new LiteralPredicate(feature, true);
                constraints.add(new Implies(selected, new Equals<>(var, new IntConstant(1L))));
                constraints.add(new Implies(new Not(selected), new Equals<>(var, new IntConstant(0L))));
                if (l == null) {
                    l = var;
                } else {
                    l = new IntAdd(l, var);
                }
            }
        }
        IntVariable count = map.addIntegerVariable("count").get();
        Equals<Long> assignment = new Equals<>(count, l);
        constraints.add(assignment);
        return new And(constraints);
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


    /**
     * Creates an alternative selection using XOR.
     *
     * @param parent        Parent feature
     * @param parseFeatures All features available for selection
     * @return parent iff (((f1 XOR f2) XOR ...) XOR fn)
     */
    protected Formula alternative(LiteralPredicate parent, ArrayList<Formula> parseFeatures) {
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
        return new Biimplies(parent, new Or(options));
    }

    /**
     * Copied from super class to overwrite the handling of alternative selections.
     *
     * @param parent
     * @param e
     * @param nodeName
     * @param and
     * @return
     * @throws ParseException
     */
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
                        constraints.add(alternative(f, parseFeatures));
                    }
                    break;
                default:
                    break;
            }
        } else if (!"feature".equals(nodeName)) {
            throw new ParseException("Empty group!");
        }

        // Add variable for counting selected features
        map.addIntegerVariable(name + COUNT_SUFFIX);

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
                map.addIntegerVariable(name);
                value = Long.parseLong(e.getAttribute("value"));
                break;
            case "double":
                map.addRealVariable(name);
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
     * Creates the attribute value assignments for all supported aggregations.
     *
     * @return Formula containing all assignments
     * @see AggregationType
     */
    protected Formula buildAssignments() {
        List<Formula> assignments = new ArrayList<>();

        for (String variable : attributeValues.keySet()) {
            Object value = attributeValues.get(variable);
            String featureName = variable.substring(variable.indexOf('(') + 1, variable.indexOf(')'));
            Optional<Variable<?>> optional = map.getVariable(featureName);
            if (optional.isPresent()) {
                BoolVariable feature = (BoolVariable) optional.get();
                LiteralPredicate selected = new LiteralPredicate(feature, true);
                for (AggregationType agg : AggregationType.values()) {
                    String aggVarName = variable + "." + agg.name();
                    if (value instanceof Long) {
                        IntVariable aggVar = map.addIntegerVariable(aggVarName).get();
                        assignments.add(new Implies(selected, new Equals<>(aggVar, (IntVariable) map.getVariable(variable).get())));
                        assignments.add(new Implies(new Not(selected), new Equals<>(aggVar, new IntConstant(agg.getDefaultValue()))));
                    } else if (value instanceof Double) {
                        RealVariable aggVar = map.addRealVariable(aggVarName).get();
                        assignments.add(new Implies(selected, new Equals<>(aggVar, (RealVariable) map.getVariable(variable).get())));
                        assignments.add(new Implies(new Not(selected), new Equals<>(aggVar, new RealConstant(agg.getDefaultValue().doubleValue()))));
                    }
                }
                if (value instanceof Long) {
                    assignments.add(new Equals<>((IntVariable) map.getVariable(variable).get(), new IntConstant((Long) value)));
                } else if (value instanceof Double) {
                    assignments.add(new Equals<>((RealVariable) map.getVariable(variable).get(), new RealConstant((Double) value)));
                }
            }
        }
        return new And(assignments);
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
