package org.spldev.formula.expression.term.attribute;

public enum AggregationType {
	MUL(1), SUM(0), AVG(0), COUNT(0);

	private final Long defaultValue;

	AggregationType(long defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Long getDefaultValue() {
		return defaultValue;
	}
}
