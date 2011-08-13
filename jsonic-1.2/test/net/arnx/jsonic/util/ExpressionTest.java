package net.arnx.jsonic.util;

import java.math.BigDecimal;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExpressionTest {
	@Test
	public void testExpression() {
		assertEquals(new BigDecimal("100.5"), Expression.compile("+100.5").evaluate());
		assertEquals(new BigDecimal("-100.5"), Expression.compile("-100.5").evaluate());
		assertEquals(new BigDecimal("58"), Expression.compile("100/2 + 1 -3 + 1*10").evaluate());
		assertEquals(new BigDecimal("1"), Expression.compile("100%3").evaluate());
		assertEquals(false, Expression.compile("!true").evaluate());
		assertEquals(true, Expression.compile("!null").evaluate());
		assertEquals(true, Expression.compile("!null || true").evaluate());
		Expression.compile("a == b ? 100 +2 : 10").evaluate();
	}
}
