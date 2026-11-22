package de.m_marvin.mathengine.operator;

import de.m_marvin.mathengine.expression.MathOperator;

/**
 * Abstract base class of every mathematical operator which can be parsed and evaluated.
 */
public abstract class BaseOperator implements MathOperator {
	
	private final String symbol;
	private final int precedence;
	private final boolean leftassociative;
	
	/**
	 * @param symbol The string from which this operator is created
	 * @param precedence The precedence level of this operator, a higher level results in priority when deciding upon evaluation order
	 * @param leftassociative Weather this operator is left associative
	 */
	public BaseOperator(String symbol, int precedence, boolean leftassociative) {
		this.symbol = symbol;
		this.precedence = precedence;
		this.leftassociative = leftassociative;
	}
	
	@Override
	public int precedence() {
		return this.precedence;
	}
	
	@Override
	public boolean leftAssociative() {
		return this.leftassociative;
	}
	
	@Override
	public String str() {
		return this.symbol;
	}

	@Override
	public String toString() {
		return "BaseOperator{ " + str() + " }";
	}
	
}
