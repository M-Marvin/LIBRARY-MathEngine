package de.m_marvin.mathengine.function;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;

import de.m_marvin.mathengine.MathematicalEvaluationException;
import de.m_marvin.mathengine.operator.DualOperation;
import de.m_marvin.mathengine.operator.DualOperationDiff;

/**
 * A class for functions with two parameters
 */
public class DualFunction extends BaseFunction {
	
	private final DualOperation operation;
	private final DualOperationDiff differentiation;
	
	/**
	 * @param name The name for the function
	 * @param operation The operation to execute on the parameters
	 * @param differentiation The operation to execute on the parameters when computing the derivative
	 */
	public DualFunction(String name, DualOperation operation, DualOperationDiff differentiation) {
		super(name);
		this.operation = operation;
		this.differentiation = differentiation;
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			evalstack.add(this.operation.apply(evalstack.pop(),  evalstack.pop()));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
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
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public String toString() {
		return "DualFunction{ " + str() + " }";
	}
	
}