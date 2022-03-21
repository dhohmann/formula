package org.spldev.formula.expression.io;

import org.junit.jupiter.api.Test;
import org.spldev.formula.expression.Formula;
import org.spldev.util.data.Result;
import org.spldev.util.io.FileHandler;
import org.spldev.util.tree.Trees;
import org.spldev.util.tree.visitor.TreePrinter;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlExtendedFeatureModelFormatTest {

    @Test
    public void testReadableFormat() {
        XmlExtendedFeatureModelFormat format = new XmlExtendedFeatureModelFormat();
        Result<Formula> result = FileHandler.load(XmlExtendedFeatureModelFormatTest.class.getResourceAsStream("/extended.xml"), format);

        assertTrue(result.isPresent());
        Trees.traverse(result.get(), new TreePrinter()).ifPresent(System.out::println);
    }
}
