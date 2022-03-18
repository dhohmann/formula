package org.spldev.formula.expression.term.integer;

import org.spldev.formula.expression.term.Divide;
import org.spldev.formula.expression.term.Term;

import java.util.List;
import java.util.Optional;

public class IntDivide extends Divide<Long> {

    public IntDivide(Term<Long> leftArgument, Term<Long> rightArgument) {
        super(leftArgument, rightArgument);
    }

    private IntDivide(){
        super();
    }

    @Override
    public Divide<Long> cloneNode() {
        return new IntDivide();
    }

    @Override
    public Optional<Long> eval(List<Long> values) {
        if (values.stream().anyMatch(value -> value == null)) {
            return Optional.empty();
        }
        return values.stream().reduce(Long::divideUnsigned);
    }

    @Override
    public Class<Long> getType() {
        return Long.class;
    }
}
