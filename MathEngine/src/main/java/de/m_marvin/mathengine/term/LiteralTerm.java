package de.m_marvin.mathengine.term;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

/**
 * A fixed numeric value for an expression.
 */
public class LiteralTerm implements MathTerm {
	
	// TODO improve caching ? maybe remove it ?
	private final static Map<Double, LiteralTerm> termCache = new HashMap<Double, LiteralTerm>();
	
	/**
	 * Returns a literal for the supplied value
	 * @param value The value for the literal
	 * @return the literal representing that value
	 */
	public static LiteralTerm valueOf(double value) {
		LiteralTerm term = termCache.get(value);
		if (term == null) {
			term = new LiteralTerm(value);
			termCache.put(value, term);
		}
		return term;
	}
	
	private final double value;
	
	private LiteralTerm(double value) {
		this.value = value;
	}

	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		evalstack.add(this.value);
	}

	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		evalstack.add(this.value);
		diffstack.add(0.0);
	}

	@Override
	public String str() {
		return Double.toString(this.value);
	}

	@Override
	public TermType type() {
		return TermType.NUMERIC;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LiteralTerm literal)
			return literal.value == this.value;
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.value);
	}
	
	@Override
	public String toString() {
		return "Literal{" + str() + "}";
	}
	
}
