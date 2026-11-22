package de.m_marvin.mathengine.function;

import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import de.m_marvin.mathengine.MathContext;
import de.m_marvin.mathengine.MathematicalEvaluationException;
import de.m_marvin.mathengine.MathematicalExpressionException;
import de.m_marvin.mathengine.expression.MathExpression;
import de.m_marvin.mathengine.expression.MathExpression.ValueAndDerivative;

/**
 * A function parsed from an infix notation, it is composed of an parameter list and an mathematical expression.
 * A function can become part of an another expression as an term, or being invoked stand alone by suppling a list of parameters.
 */
public class MathFunction extends BaseFunction {
	
	private final List<String> variables;
	private final MathExpression expression;
	
	/**
	 * @param name The name for the function
	 * @param expression The expression which computes the result of the function from its parameters
	 * @param variables The list of variables the expression expects, for correct evaluation they have to be passed in the same order
	 */
	public MathFunction(String name, MathExpression expression, List<String> variables) {
		super(name);
		this.variables = Collections.unmodifiableList(variables);
		this.expression = expression;
	}
	
	public static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\s*\\(\\s*(\\w+(?:\\s*,\\s*\\w+)*)\\s*\\)\\s*:=\\s*(.*)", Pattern.DOTALL);
	
	/**
	 * Parses an single function defined as infix notation with the supplied context and constructs a new function object which can be evaluated.
	 * The created function instance is also added to the context so it can be used in further function that are being parsed with the same context.
	 * The variable list in the context will be cleared before parsing the function, and will contain the variables only of this function after completion.
	 * @param s The infix notation of the function
	 * @param ctx The parsing context.
	 * @return The newly created function instance
	 * @throws MathematicalExpressionException If the function is malformed, note that not all cases of ill-formed function are captured, some will only show up during evaluation
	 */
	public static MathFunction parseInfix(String s, MathContext ctx) {

		Matcher m = FUNCTION_PATTERN.matcher(s);
		if (!m.find())
			throw new MathematicalExpressionException("not a valid function syntax: " + s);
		
		return parseInfix(m, ctx);
	}
	
	public static MathFunction parseInfix(Matcher m, MathContext ctx) {
		
		try {

			ctx.clearVariablesUsed();
			
			String name = m.group(1);
			List<String> variables = Stream.of(m.group(2).split(",")).map(String::strip).toList();
			MathExpression expression = MathExpression.parseInfix(m.group(3), ctx);
			
			if (!variables.containsAll(ctx.variablesUsed()))
				throw new MathematicalExpressionException("function expression uses undefined variables (used/defined): " + ctx.variablesUsed() + "/" + variables);
			
			return new MathFunction(name, expression, variables);
			
		} catch (MathematicalExpressionException e) {
			throw new MathematicalExpressionException("unable to parse function expression: " + m.group(), e);
		}
		
	}
	
	/**
	 * Returns the list of variables defined for this function, in the order in which they where defined and are accepted by the evaluation methods.
	 * @return A ordered list of the variables of this function.
	 */
	public List<String> variables() {
		return this.variables;
	}
	
	/**
	 * Returns the expression which computes the results of this function.
	 * @return The expression instance of this function
	 */
	public MathExpression expression() {
		return this.expression;
	}

	/**
	 * Evaluates the results of this function.
	 * @param parameters The list of parameters (variable values) for this evaluation
	 */
	public double evaluat(double... parameters) {
		if (parameters.length != this.variables.size())
			throw new IllegalArgumentException("parameter count does not match variable count");
		
		Map<String, Double> parameterMap = new HashMap<String, Double>();
		for (int i = 0; i < parameters.length; i++)
			parameterMap.put(this.variables.get(i), parameters[i]);
		
		return evaluate(parameterMap);
	}

	/**
	 * Evaluates the results of this function and its partial derivative in respect to the given variable.
	 * @param variable The variable for which the partial derivative should be computed
	 * @param parameters The list of parameters (variable values) for this evaluation
	 */
	public ValueAndDerivative evaluateAndDerive(String variable, double... parameters) {
		if (parameters.length != this.variables.size())
			throw new IllegalArgumentException("parameter count does not match variable count");
		
		Map<String, Double> parameterMap = new HashMap<String, Double>();
		for (int i = 0; i < parameters.length; i++)
			parameterMap.put(this.variables.get(i), parameters[i]);
		
		return evaluateAndDerive(variable, parameterMap);
	}

	/**
	 * Evaluates the results of this function and its partial derivative in respect to the given variable.
	 * @param variable The variable index for which the partial derivative should be computed
	 * @param parameters The list of parameters (variable values) for this evaluation
	 */
	public ValueAndDerivative evaluateAndDerive(int variable, double... parameters) {
		if (parameters.length != this.variables.size())
			throw new IllegalArgumentException("parameter count does not match variable count");
		
		if (variable >= this.variables.size() || variable < 0)
			throw new IndexOutOfBoundsException("variable index out of range [0-" + this.variables.size() + ")");
		
		Map<String, Double> parameterMap = new HashMap<String, Double>();
		for (int i = 0; i < parameters.length; i++)
			parameterMap.put(this.variables.get(i), parameters[i]);
		
		return evaluateAndDerive(this.variables.get(variable), parameterMap);
	}

	/**
	 * Evaluates the results of this function.
	 * @param parameters The parameters (variable values) for this evaluation
	 */
	public double evaluate(Map<String, Double> parameters) {
		return this.expression.evaluate(parameters);	
	}
	
	/**
	 * Evaluates the results of this function and its partial derivative in respect to the given variable.
	 * @param variable The variable for which the partial derivative should be computed
	 * @param parameters The parameters (variable values) for this evaluation
	 */
	public ValueAndDerivative evaluateAndDerive(String variable, Map<String, Double> parameters) {
		return this.expression.evaluateAndDerive(parameters, variable);
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			double[] parameterList = new double[this.variables.size()];
			for (int i = 0; i < parameterList.length; i++)
				parameterList[parameterList.length - i - 1] = evalstack.pop();
			evalstack.add(evaluat(parameterList));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		try {
			double[] parameterList = new double[this.variables.size()];
			double[] parameterListDiff = new double[this.variables.size()];
			for (int i = parameterList.length - 1; i >= 0; i--) {
				parameterList[i] = evalstack.pop();
				parameterListDiff[i] = diffstack.pop();
			}
			
			double derivative = 0.0;
			Double value = null;
			for (int i = 0; i < parameterList.length; i++) {
				if (parameterListDiff[i] == 0.0)
					continue;
				ValueAndDerivative result = evaluateAndDerive(i, parameterList);
				derivative += result.derivative() * parameterListDiff[i];
				if (value == null)
					value = result.value();
			}
			
			if (value == null)
				value = evaluat(parameterList);
			
			evalstack.add(value);
			diffstack.add(derivative);
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public String toString() {
		return "MathFunction{ " + str() + " := " + this.expression.str() + " }";
	}
	
}
