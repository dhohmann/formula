package org.spldev.formula.expression.term.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;

public abstract class Product<D> extends Aggregation<D> {

	public Product(String name, VariableMap variableMap) {
		super(name, variableMap);
	}

	protected Product() {
		super();
	}

	@Override
	public String getName() {
		return "mul";
	}

	@Override
	public abstract Product<D> cloneNode();

	@Override
	protected AggregationType getAggregationType() {
		return AggregationType.MUL;
	}
}
