package org.spldev.formula.expression.term.integer.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.attribute.Sum;

import java.util.List;
import java.util.Optional;

/**
 * Builds a sum over a feature attribute.
 * Requires the variables for each attribute to be in the form <code>attribute(feature).sum</code>
 */
public class IntSum extends Sum<Long> {

    public IntSum(String featureAttribute, VariableMap variableMap) {
        super(featureAttribute, variableMap);
    }

    protected IntSum() {
        super();
    }

    @Override
    public Optional<Long> eval(List<Long> values) {
        if (values.stream().anyMatch(value -> value == null)) {
            return Optional.empty();
        }
        return values.stream().reduce(Long::sum);
    }

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public IntSum cloneNode() {
        return new IntSum();
    }
}
