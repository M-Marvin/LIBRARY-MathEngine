package de.m_marvin.mathengine.term;

import java.util.Map;
import java.util.Stack;

/**
 * A constant value.
 * This is equivalent to a {@link LiteralTerm}, but with an assigned name.
 */
public class ConstantTerm implements MathTerm {
	
	private final String name;
	private final double value;
	
	/**
	 * @param name The name for the constant
	 * @param value The value for the constant
	 */
	public ConstantTerm(String name, double value) {
		this.name = name;
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
		return this.name;
	}

	@Override
	public TermType type() {
		return TermType.NUMERIC;
	}

	@Override
	public String toString() {
		return "Constant{" + str() + "}";
	}

}
