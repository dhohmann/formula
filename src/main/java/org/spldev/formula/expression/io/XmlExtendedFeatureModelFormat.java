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
import org.spldev.formula.expression.compound.And;
import org.spldev.formula.expression.compound.Implies;
import org.spldev.formula.expression.compound.Not;
import org.spldev.formula.expression.compound.Or;
import org.spldev.formula.expression.term.Variable;
import org.spldev.formula.expression.term.attribute.AggregationType;
import org.spldev.formula.expression.term.bool.BoolVariable;
import org.spldev.formula.expression.term.integer.IntConstant;
import org.spldev.formula.expression.term.integer.IntVariable;
import org.spldev.formula.expression.term.real.RealConstant;
import org.spldev.formula.expression.term.real.RealVariable;
import org.spldev.util.io.format.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class XmlExtendedFeatureModelFormat extends XmlFeatureModelFormat {

    public static final String ID = XmlExtendedFeatureModelFormat.class.getCanonicalName();

    protected final static String FEATURE_MODEL = "extendedFeatureModel";
    protected final static String ATTRIBUTE = "attribute";

    private final HashMap<String, Object> attributeValues = new HashMap(); // TODO: Eigene Klasse für Features, Attr, werte

    public XmlExtendedFeatureModelFormat() {
        super();
    }


    protected Formula readDocument(Document doc) throws ParseException {
        map = VariableMap.emptyMap();
        final List<Element> elementList = getElement(doc, FEATURE_MODEL);
        if (elementList.size() == 1) {
            final Element e = elementList.get(0);
            parseStruct(getElement(e, STRUCT));
            parseConstraints(getElement(e, CONSTRAINTS));
        } else if (elementList.isEmpty()) {
            throw new ParseException("Not a feature model xml element!");
        } else {
            throw new ParseException("More than one feature model xml elements!");
        }

        // TODO Geht mit NON-SMT-Solvern nicht
        if(!attributeValues.isEmpty()){
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


    protected LiteralPredicate parseFeature(Literal parent, final Element e, final String nodeName, boolean and)
            throws ParseException {

        LiteralPredicate f = super.parseFeature(parent, e, nodeName, and);
        String name = null;
        if (e.hasAttributes()) {
            final NamedNodeMap nodeMap = e.getAttributes();
            for (int i = 0; i < nodeMap.getLength(); i++) {
                final org.w3c.dom.Node node = nodeMap.item(i);
                final String attributeName = node.getNodeName();
                final String attributeValue = node.getNodeValue();
                if (attributeName.equals(NAME)) {
                    name = attributeValue;
                }
            }
        }
        if (name == null) {
            throw new ParseException("Parent feature has no name");
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

    protected Formula buildAssignments() {
        List<Formula> assignments = new ArrayList<>();

        for (String variable : attributeValues.keySet()) {
            Object value = attributeValues.get(variable);
            String featureName = variable.substring(0, variable.indexOf('('));
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
        return new XmlExtendedFeatureModelFormat();
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
