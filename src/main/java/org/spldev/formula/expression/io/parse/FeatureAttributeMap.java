package org.spldev.formula.expression.io.parse;

import org.spldev.formula.expression.Formula;
import org.spldev.formula.expression.atomic.literal.LiteralPredicate;
import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.atomic.predicate.Equals;
import org.spldev.formula.expression.compound.And;
import org.spldev.formula.expression.compound.Implies;
import org.spldev.formula.expression.compound.Not;
import org.spldev.formula.expression.term.Term;
import org.spldev.formula.expression.term.Variable;
import org.spldev.formula.expression.term.attribute.AggregationType;
import org.spldev.formula.expression.term.bool.BoolVariable;
import org.spldev.formula.expression.term.integer.IntAdd;
import org.spldev.formula.expression.term.integer.IntConstant;
import org.spldev.formula.expression.term.integer.IntVariable;
import org.spldev.formula.expression.term.real.RealConstant;
import org.spldev.formula.expression.term.real.RealVariable;
import org.spldev.util.io.format.ParseException;

import java.util.*;

public class FeatureAttributeMap {

	public static class FeatureAttribute {

		private final FeatureAttributeMap map;
		private final String name;
		private final Class<?> type;

		public FeatureAttribute(FeatureAttributeMap map, String name, Class<?> type) {
			this.name = name;
			this.type = type;
			this.map = map;
		}

		public Class<?> getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public Optional<Value> getValueFor(String feature) {
			return map.getValue(name, feature);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			FeatureAttribute that = (FeatureAttribute) o;
			return map.equals(that.map) && name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(map, name);
		}
	}

	public static class Value {
		private final String feature;
		private final Object value;

		public Value(String feature, Object value) {
			this.feature = feature;
			this.value = value;
		}

		public Object getValue() {
			return value;
		}

		public String getFeature() {
			return feature;
		}
	}

	private final List<FeatureAttribute> attributes;
	private final Map<FeatureAttribute, Set<Value>> values;

	public FeatureAttributeMap() {
		attributes = new ArrayList<>();
		values = new LinkedHashMap<>();
	}

	public void addFeatureAttribute(String attribute, String feature, Object value) {
		Optional<FeatureAttribute> attr = getAttribute(attribute);
		if (!attr.isPresent()) {
			attr = Optional.of(createAttribute(attribute, value.getClass()));
		}
		Set<Value> attrValues = values.get(attr);
		Value v = new Value(feature, value);
		if (!attrValues.add(v)) {
			throw new RuntimeException("Duplicate feature attribute value");
		}
	}

	protected FeatureAttribute createAttribute(String name, Class<?> type) {
		FeatureAttribute attr = new FeatureAttribute(this, name, type);
		if (attributes.contains(attr)) {
			throw new RuntimeException("Duplicate attribute definition for " + name);
		}
		if (values.containsKey(attr)) {
			throw new RuntimeException("Values for attribute " + name + " already defined");
		}
		attributes.add(attr);
		values.put(attr, new HashSet<>());
		return attr;
	}

	public Optional<FeatureAttribute> getAttribute(String name) {
		for (FeatureAttribute attribute : attributes) {
			if (attribute.name.equals(name)) {
				return Optional.of(attribute);
			}
		}
		return Optional.empty();
	}

	public Optional<Value> getValue(String attribute, String feature) {
		Optional<FeatureAttribute> attr = getAttribute(attribute);
		if (!attr.isPresent()) {
			throw new RuntimeException("Attribute " + attribute + " does not exist");
		}
		if (!values.containsKey(attr)) {
			throw new RuntimeException("Attribute " + attribute + " has no values");
		}
		Set<Value> attrValues = values.get(attr);
		for (Value v : attrValues) {
			if (v.feature.equals(feature)) {
				return Optional.of(v);
			}
		}
		return Optional.empty();
	}

	public boolean isEmpty() {
		return attributes.isEmpty();
	}

}
