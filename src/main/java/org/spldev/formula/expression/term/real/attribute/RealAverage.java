package org.spldev.formula.expression.term.real.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.attribute.Average;

import java.util.List;
import java.util.Optional;

public class RealAverage extends Average<Double> {

    public RealAverage(String name, VariableMap map){
        super(name, map);
    }

    protected RealAverage(){
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
    public Average<Double> cloneNode() {
        return new RealAverage();
    }
}
