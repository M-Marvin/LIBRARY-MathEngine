package de.m_marvin.mathengine.function;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;

import de.m_marvin.mathengine.MathematicalEvaluationException;
import de.m_marvin.mathengine.operator.SingleOperation;
import de.m_marvin.mathengine.operator.SingleOperationDiff;

/**
 * A class for functions with a single parameter
 */
public class SingleFunction extends BaseFunction {
	
	private final SingleOperation function;
	private final SingleOperationDiff differentiation;

	/**
	 * @param name The name for the function
	 * @param operation The operation to execute on the parameter
	 * @param differentiation The operation to execute on the parameter when computing the derivative
	 */
	public SingleFunction(String name, SingleOperation function, SingleOperationDiff differentiation) {
		super(name);
		this.function = function;
		this.differentiation = differentiation;
	}

	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			evalstack.add(this.function.apply(evalstack.pop()));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		try {
			double x = evalstack.pop();
			evalstack.add(this.function.apply(x));
			if (this.differentiation != null)
				diffstack.add(this.differentiation.apply(x, diffstack.pop()));
			else
				diffstack.add(evalstack.peek());
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public String toString() {
		return "SingleFunction{ " + str() + " }";
	}
	
}