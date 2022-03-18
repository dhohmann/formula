package org.spldev.formula.expression.term.real.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.attribute.Sum;

import java.util.List;
import java.util.Optional;

public class RealSum extends Sum<Double> {

    public RealSum(String name, VariableMap variableMap) {
        super(name, variableMap);
    }

    protected RealSum() {
        super();
    }

    @Override
    public Optional<Double> eval(List<Double> values) {
        if (values.stream().anyMatch(value -> value == null)) {
            return Optional.empty();
        }
        return values.stream().reduce(Double::sum);
    }

    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public Sum<Double> cloneNode() {
        return new RealSum();
    }
}
