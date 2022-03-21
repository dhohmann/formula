package org.spldev.formula.expression.term.real.attribute;

import org.spldev.formula.expression.atomic.literal.VariableMap;
import org.spldev.formula.expression.term.attribute.Product;

import java.util.List;
import java.util.Optional;

public class RealProduct extends Product<Double> {

    public RealProduct(String name, VariableMap map){
        super(name, map);
    }

    protected RealProduct(){
        super();
    }

    @Override
    public Optional<Double> eval(List<Double> values) {
        return Optional.empty();
    }

    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public Product<Double> cloneNode() {
        return new RealProduct();
    }
}
