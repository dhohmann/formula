package org.spldev.formula.expression.term.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;

public abstract class Average<D> extends Aggregation<D> {

	public Average(String featureAttribute, VariableMap variables) {
		super(featureAttribute, variables);
		if (!variables.hasVariable("count")) {
			throw new UnsupportedOperationException("Average requires the variable count to be present.");
		}
	}

	protected Average() {
		super();
	}

	@Override
	protected AggregationType getAggregationType() {
		return AggregationType.AVG;
	}

	@Override
	public String getName() {
		return "avg";
	}

	@Override
	public abstract Average<D> cloneNode();

}
