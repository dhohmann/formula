package org.spldev.formula.expression.io;

import org.junit.jupiter.api.Test;
import org.spldev.formula.expression.Formula;
import org.spldev.formula.expression.compound.And;
import org.spldev.util.data.Result;
import org.spldev.util.io.FileHandler;
import org.spldev.util.tree.Trees;
import org.spldev.util.tree.visitor.TreePrinter;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ConstraintTest {

	@Test
	public void testParsing() {
		XmlExtendedFeatureModelFormat format = new XmlExtendedFeatureModelFormat();
		Result<Formula> result = FileHandler.load(ConstraintTest.class.getResourceAsStream("/extended.xml"), format);

		assumeTrue(result.isPresent());
		ConfiguringConstraintsFormat constraintsFormat = new ConfiguringConstraintsFormat(result.get()
			.getVariableMap());
		Result<Formula> constraints = FileHandler.load(ConstraintTest.class.getResourceAsStream(
			"/aggregationConstraints.xml"), constraintsFormat);

		assertEquals(Collections.emptyList(), constraints.getProblems());
		assertTrue(constraints.get() instanceof And);
		assertTrue(constraints.get().hasChildren());

		//Trees.traverse(constraints.get(), new TreePrinter()).ifPresent(System.out::println);
	}
}
