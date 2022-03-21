package org.spldev.formula.expression.term.integer.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.attribute.Average;

import java.util.List;
import java.util.Optional;

public class IntAverage extends Average<Long> {

    public IntAverage(String name, VariableMap map){
        super(name, map);
    }

    protected IntAverage(){
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
    public Average<Long> cloneNode() {
        return new IntAverage();
    }
}
