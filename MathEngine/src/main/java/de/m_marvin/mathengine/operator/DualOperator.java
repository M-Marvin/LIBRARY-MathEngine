package de.m_marvin.mathengine.operator;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;

import de.m_marvin.mathengine.MathematicalEvaluationException;

/**
 * Class for dual (two operand) operators, extends {@link BaseOperator}.
 */
public class DualOperator extends BaseOperator {
	
	private final DualOperation operation;
	private final DualOperationDiff differentiation;
	
	/**
	 * 
	 * @param symbol The string from which this operator is created
	 * @param precedence The precedence level of this operator, a higher level results in priority when deciding upon evaluation order
	 * @param leftassociative Weather this operator is left associative
	 * @param operation The operation to perform on the two operands
	 * @param differentiation The operation to perform on the two operands when computing the derivative
	 */
	public DualOperator(String symbol, int precedence, boolean leftassoative, DualOperation operation, DualOperationDiff differentiation) {
		super(symbol, precedence, leftassoative);
		this.operation = operation;
		this.differentiation = differentiation;
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			evalstack.add(this.operation.apply(evalstack.pop(),  evalstack.pop()));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
		}
	}
	
	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		try {
			double B = evalstack.pop();
			double A = evalstack.pop();
			evalstack.add(this.operation.apply(B, A));
			if (this.differentiation != null)
				diffstack.add(this.differentiation.apply(B, diffstack.pop(), A, diffstack.pop()));
			else
				diffstack.add(evalstack.peek());
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
		}
	}

	@Override
	public String toString() {
		return "DualOperator{ " + str() + " }";
	}
	
}