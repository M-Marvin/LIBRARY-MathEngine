package de.m_marvin.mathengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;

import de.m_marvin.mathengine.expression.MathExpression;
import de.m_marvin.mathengine.expression.MathExpression.ValueAndDerivative;
import de.m_marvin.mathengine.expression.MathOperator;
import de.m_marvin.mathengine.function.BaseFunction;
import de.m_marvin.mathengine.function.DualFunction;
import de.m_marvin.mathengine.function.MathFunction;
import de.m_marvin.mathengine.function.SingleFunction;
import de.m_marvin.mathengine.operator.BaseOperator;
import de.m_marvin.mathengine.operator.DualOperator;
import de.m_marvin.mathengine.term.ConstantTerm;
import de.m_marvin.mathengine.term.LiteralTerm;
import de.m_marvin.mathengine.term.MathTerm;
import de.m_marvin.mathengine.term.VariableTerm;

/**
 * An implementation of the {@link MathContext} interface which also supplies utility methods for parsing both single and lists of infix notations.
 */
public class MathParser implements MathContext {

	private MathParser() {}
	
	/**
	 * Create a new parser with no standard operators, functions or constants registered.
	 * @return a new parser instance
	 */
	public static MathParser empty() {
		return new MathParser();
	}
	
	/**
	 * Create a new parser with the standard operators, functions and constants registered.
	 * @return a new parser instance
	 */
	public static MathParser standard() {
		return empty().addStandardOperators().addStandardFunctions();
	}
	
	/**
	 * Assign the variable map to this context, the instance will be references by this context, thus changes to the list will be reflected in the context.
	 * @param parameters The parameter list to use
	 */
	public MathParser withParameters(Map<String, Double> parameters) {
		this.parameters = parameters;
		return this;
	}
	
	/**
	 * Adds all the standard operators to this context.
	 */
	public MathParser addStandardOperators() {
		
		register(new DualOperator("+", 4, true, (B, A) -> A + B, (B, Bd, A, Ad) -> Ad + Bd));
		register(new DualOperator("-", 4, true, (B, A) -> A - B, (B, Bd, A, Ad) -> Ad - Bd));
		register(new DualOperator("*", 5, true, (B, A) -> A * B, (B, Bd, A, Ad) -> Ad * B + A * Bd));
		register(new DualOperator("/", 5, true, (B, A) -> A / B, (B, Bd, A, Ad) -> (Ad * B + A * Bd) / (B * B)));
		register(new DualOperator("^", 6, false, (B, A) -> Math.pow(A, B), (B, Bd, A, Ad) -> B * Math.pow(A, B - 1)));
		register(new DualOperator("%", 5, false, (B, A) -> A % B, (B, Bd, A, Ad) -> { throw new MathematicalEvaluationException("modulo not differentiable"); }));
		
		register(new DualOperator("=", 1, true, (B, A) -> Double.compare(A, B) == 0 ? 1.0 : 0.0, null));
		register(new DualOperator(">", 1, true, (B, A) -> A > B ? 1.0 : 0.0, null));
		register(new DualOperator("<", 1, true, (B, A) -> A < B ? 1.0 : 0.0, null));
		register(new DualOperator(">=", 1, true, (B, A) -> A >= B ? 1.0 : 0.0, null));
		register(new DualOperator("<=", 1, true, (B, A) -> A <= B ? 1.0 : 0.0, null));
		register(new DualOperator("!=", 1, true, (B, A) -> Double.compare(A, B) != 0 ? 1.0 : 0.0, null));
		
		register(new BaseOperator("!", 6, false) {
			
			@Override
			public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
				try {
					double A = evalstack.pop();
					evalstack.add(A != 0.0 ? 0.0 : 1.0);
				} catch (EmptyStackException e) {
					throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
				}
			}
			
			@Override
			public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
				evaluate(evalstack, parameters);
				diffstack.add(evalstack.peek());
			}
			
		});
		
		register(new DualOperator("&", 3, true, (B, A) -> (A != 0.0) && (B  != 0.0) ? 1.0 : 0.0, null));
		register(new DualOperator("|", 2, true, (B, A) -> (A != 0.0) || (B != 0.0) ? 1.0 : 0.0, null));
		register(new DualOperator("°", 2, true, (B, A) -> (A != 0.0) ^  (B != 0.0) ? 1.0 : 0.0, null));
		
		register(new BaseOperator("?", 0, true) {
			
			@Override
			public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
				try {
					double A = evalstack.pop();
					double B = evalstack.pop();
					double C = evalstack.pop();
					evalstack.add(C != 0.0 ? B : A);
				} catch (EmptyStackException e) {
					throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
				}
			}
			
			@Override
			public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
				evaluate(evalstack, parameters);
				try {
					double Ad = diffstack.pop();
					double Bd = diffstack.pop();
					double Cd = diffstack.pop();
					diffstack.add(Cd != 0.0 ? Bd : Ad);
				} catch (EmptyStackException e) {
					throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
				}
			}
			
			@Override
			public boolean implicitBracket() {
				return true;
			}
			
		});
		
		return this;
	}
	
	/**
	 * Adds all the standard constants to this context.
	 */
	public MathParser addStandardConstants() {
		
		register(new ConstantTerm("pi", Math.PI));
		register(new ConstantTerm("e", Math.E));
		register(new ConstantTerm("tau", Math.TAU));
		
		return this;
	}
	
	/**
	 * Adds all the standard functions to this context.
	 */
	public MathParser addStandardFunctions() {
		
		register(new SingleFunction("sin", (x) -> Math.sin(x), (x, xd) -> Math.cos(x) * xd));
		register(new SingleFunction("cos", (x) -> Math.cos(x), (x, xd) -> -Math.sin(x) * xd));
		register(new SingleFunction("tan", (x) -> Math.tan(x), (x, xd) -> (1 + Math.pow(Math.tan(x), 2)) * xd));
		register(new SingleFunction("asin", (x) -> Math.asin(x), (x, xd) -> (1 / Math.sqrt(1 - Math.pow(x, 2))) * xd));
		register(new SingleFunction("acos", (x) -> Math.acos(x), (x, xd) -> (-1 / Math.sqrt(1 - Math.pow(x, 2))) * xd));
		register(new SingleFunction("atan", (x) -> Math.atan(x), (x, xd) -> (1 / (1 + Math.pow(x, 2))) * xd));
		
		register(new SingleFunction("log", (x) -> Math.log(x), (x, xd) -> (1 / x) * xd));
		register(new SingleFunction("log10", (x) -> Math.log10(x), (x, xd) -> (1 / (x * Math.log(10))) * xd));
		register(new SingleFunction("exp", (x) -> Math.exp(x), (x, xd) -> Math.exp(x) * xd));
		register(new SingleFunction("sqrt", (x) -> Math.sqrt(x), (x, xd) -> (0.5 * Math.pow(x, -1.5)) * xd));
		register(new DualFunction("pow", (B, A) -> Math.pow(A, B), (B, Bd, A, Ad) -> (B * Math.pow(A, B - 1) * Ad) + (Math.pow(A, B) * Math.log(A) * Bd)));
		
		register(new SingleFunction("abs", (X) -> Math.abs(X), (X, Xd) -> X < 0 ? -Xd : Xd));
		register(new SingleFunction("ceil", (x) -> Math.ceil(x), (x, xd) -> 0.0));
		register(new SingleFunction("floor", (x) -> Math.floor(x), (x, xd) -> 0.0));
		register(new SingleFunction("round", (x) -> Math.round(x), (x, xd) -> 0.0));
		register(new DualFunction("min", (B,  A) -> Math.min(A, B), (B, Bd, A, Ad) -> A <= B ? Ad : Bd));
		register(new DualFunction("max", (B,  A) -> Math.max(A, B), (B, Bd, A, Ad) -> A >= B ? Ad : Bd));
		
		return this;
	}
	
	/**
	 * Parses a single infix notation which either represents a bare expression, an constant assignment or an function declaration.
	 * Only bare expressions will be returned for evaluation, other entries will be registered for this context.
	 * @param infixStr The infix notation to parse
	 * @return The bare expression or null if none exists
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 */
	public MathExpression parseInfix(String infixStr) {
		Objects.requireNonNull(infixStr, "infixStr");
		Matcher m;
		if ((m = MathFunction.FUNCTION_PATTERN.matcher(infixStr)).find())
			register(MathFunction.parseInfix(m, this));
		else if ((m = MathExpression.ASSIGNMENT_PATTERN.matcher(infixStr)).find())
			MathExpression.parseAndEvaluateAssignment(m, this);
		else
			return MathExpression.parseInfix(infixStr, this);
		return null;
	}

	/**
	 * Same as {@link MathParser#parseInfix(String)}, but also evaluates the value of the parsed expression
	 * using the current parameter map of this context and returns the result.
	 * @param infixStr The infix notation to parse
	 * @return the value to which the expression evaluated
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 * @throws NoSuchElementException if the supplied notation does not contain any bare expression to evaluate
	 */
	public double parseInfixAndEvaluate(String infixStr) {
		MathExpression expr = parseInfix(infixStr);
		if (expr == null)
			throw new NoSuchElementException("infix does not result in expression to evaluate");
		return expr.evaluate(varmap());
	}

	/**
	 * Same as {@link MathParser#parseInfixAndEvaluate(String)}, but also evaluates the partial derivative value of the parsed expression for the supplied variable name
	 * using the current parameter map of this context and returns the result.
	 * @param infixStr the infix notation to parse
	 * @param variable the name of the variable to compute the partial derivative for
	 * @return the value to which the expression evaluated
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 * @throws NoSuchElementException if the supplied notation does not contain any bare expression to evaluate
	 */
	public ValueAndDerivative parseInfixAndEvaluateAndDerive(String infixStr, String variable) {
		MathExpression expr = parseInfix(infixStr);
		if (expr == null)
			throw new NoSuchElementException("infix does not result in expression to evaluate");
		return expr.evaluateAndDerive(varmap(), variable);
	}

	/**
	 * Parses a list of infix notations which either represents a bare expression, an constant assignment or an function declaration.
	 * The first occurring bare expression is returned as a result and will also terminate parsing of further list entries.
	 * @param infixStr The infix notation list to parse
	 * @return The first bare expression or null if none was found
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 */
	public MathExpression parseInfixList(String infixStr) {
		Objects.requireNonNull(infixStr, "infixStr");
		for (String s1 : infixStr.split(";")) {
			if (s1.isBlank())
				continue;
			
			MathExpression expr = parseInfix(s1.strip());
			if (expr != null)
				return expr;
		}
		return null;
	}

	/**
	 * Same as {@link MathParser#parseInfixList(String)}, but also evaluates the value of the parsed expression
	 * using the current parameter map of this context and returns the result.
	 * @param infixStr The infix notation list to parse
	 * @return the value to which the expression evaluated
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 * @throws NoSuchElementException if the supplied notation does not contain any bare expression to evaluate
	 */
	public double parseInfixListAndEvaluate(String infixStr) {
		MathExpression expr = parseInfixList(infixStr);
		if (expr == null)
			throw new NoSuchElementException("infix does not result in expression to evaluate");
		return expr.evaluate(varmap());
	}

	/**
	 * Same as {@link MathParser#parseInfixListAndEvaluate(String)}, but also evaluates the partial derivative value of the parsed expression for the supplied variable name
	 * using the current parameter map of this context and returns the result.
	 * @param infixStr the infix notation list to parse
	 * @param variable the name of the variable to compute the partial derivative for
	 * @return the value to which the expression evaluated
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 * @throws NoSuchElementException if the supplied notation does not contain any bare expression to evaluate
	 */
	public ValueAndDerivative parseInfixListAndEvaluateAndDerive(String infixStr, String variable) {
		MathExpression expr = parseInfixList(infixStr);
		if (expr == null)
			throw new NoSuchElementException("infix does not result in expression to evaluate");
		return expr.evaluateAndDerive(varmap(), variable);
	}
	
	private Map<String, Double> parameters;
	private final Map<String, MathTerm> objects = new HashMap<String, MathTerm>();
	private final Map<String, MathOperator> operators = new HashMap<String, MathOperator>();
	private List<String> usedVariables;
	
	@Override
	public Map<String, Double> varmap() {
		if (this.parameters == null)
			return Collections.emptyMap();
		return this.parameters;
	}

	@Override
	public MathTerm literal(double value) {
		return LiteralTerm.valueOf(value);
	}

	@Override
	public MathTerm variable(String str) {
		try {
			return VariableTerm.valueOf(str);
		} finally {
			this.usedVariables.add(str);
		}
	}

	@Override
	public Collection<String> variablesUsed() {
		return this.usedVariables;
	}

	@Override
	public void clearVariablesUsed() {
		this.usedVariables = new ArrayList<String>();
	}

	@Override
	public void register(MathTerm object) {
		Objects.requireNonNull(object, "object");
		if (object instanceof MathOperator operator)
			this.operators.put(object.str(), operator);
		else if (object instanceof MathFunction || object instanceof ConstantTerm || object instanceof BaseFunction)
			this.objects.put(object.str(), object);
		else
			throw new IllegalArgumentException("object is not a function, constant or operator");
	}

	@Override
	public MathTerm constant(String str) {
		return this.objects.get(str);
	}

	@Override
	public MathTerm function(String str) {
		return this.objects.get(str + "()");
	}

	@Override
	public MathOperator operator(String str) {
		return this.operators.get(str);
	}
	
}
