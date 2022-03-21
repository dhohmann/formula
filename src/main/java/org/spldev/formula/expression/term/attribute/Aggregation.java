package org.spldev.formula.expression.term.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.Function;
import org.spldev.formula.expression.term.Term;

import java.util.ArrayList;
import java.util.List;

public abstract class Aggregation<D> extends Function<D, D> {

    /**
     * Builds the aggregation over a feature attribute using the provided variables.
     * The variables must match the format <code>attribute(feature)</code>.
     *
     * @param featureAttribute Attribute name to build the aggregation on
     * @param variables        Available variables containing the attribute variables
     */
    public Aggregation(String featureAttribute, VariableMap variables) {
        List<Term<D>> sumElements = new ArrayList<>();
        String format = featureAttribute + "\\(.*\\)\\." + getAggregationType().name();

        for (VariableMap.VariableSignature s : variables) {
            if(s == null) continue;
            if (s.getName().matches(format)) {
                sumElements.add((Term<D>) variables.getVariable(s.getName()).get());
            }
        }
        setChildren(sumElements);
    }

    protected Aggregation() {
        super();
    }

    /**
     * Specifies the supported aggregation type.
     *
     * @return Supported aggregation type
     */
    protected abstract AggregationType getAggregationType();
}
