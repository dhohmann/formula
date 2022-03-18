package org.spldev.formula.expression.term;

public abstract class Divide<T> extends Function<T, T>{

    public Divide(Term<T> leftArgument, Term<T> rightArgument) {
        super(leftArgument, rightArgument);
    }

    protected Divide(){
        super();
    }

    @Override
    public String getName() { return "/ "; }

    @Override
    public abstract Divide<T> cloneNode();
}
