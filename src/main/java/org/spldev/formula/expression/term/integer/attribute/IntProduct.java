package org.spldev.formula.expression.term.integer.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.attribute.Product;

import java.util.List;
import java.util.Optional;

public class IntProduct extends Product<Long> {

	public IntProduct(String name, VariableMap map) {
		super(name, map);
	}

	protected IntProduct() {
		super();
	}

	@Override
	public Optional<Long> eval(List<Long> values) {
		return Optional.empty();
	}

	@Override
	public Class<Long> getType() {
		return Long.class;
	}

	@Override
	public Product<Long> cloneNode() {
		return new IntProduct();
	}
}
