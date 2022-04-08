package org.spldev.formula.expression.term.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;

public abstract class Sum<D> extends Aggregation<D> {

	public Sum(String name, VariableMap variables) {
		super(name, variables);
	}

	protected Sum() {
		super();
	}

	@Override
	protected AggregationType getAggregationType() {
		return AggregationType.SUM;
	}

	@Override
	public String getName() {
		return "sum";
	}

	@Override
	public abstract Sum<D> cloneNode();

}
