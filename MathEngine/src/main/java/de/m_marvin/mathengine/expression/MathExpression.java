package de.m_marvin.mathengine.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.m_marvin.mathengine.MathContext;
import de.m_marvin.mathengine.MathematicalEvaluationException;
import de.m_marvin.mathengine.MathematicalExpressionException;
import de.m_marvin.mathengine.term.ConstantTerm;
import de.m_marvin.mathengine.term.MathTerm;

/**
 * An expression is composed of one or more {@link MathTerm}'s in a sensible arrangement.
 * An expression can also be used as an term inside an another expression, in which case the expression might not even be able to be evaluated outside of one because entries in the evaluation stack are required (incomplete expression).
 */
public class MathExpression implements MathTerm {
	
	private final List<MathTerm> postfixTerms;

	/**
	 * Constructs a new expression from the list of terms in postfix notation.
	 * @param postixTerms The terms for the expression
	 */
	public MathExpression(List<MathTerm> postixTerms) {
		this.postfixTerms = Collections.unmodifiableList(postixTerms);
	}
	
	/**
	 * A placeholder term which is used during parsing to mark where a bracket was opened.
	 * No STRUCTURE type terms will ever show up in the final expression.
	 */
	private static final MathTerm BRACKET_PLACEHOLDER = new MathTerm() {

		@Override
		public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {}

		@Override
		public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {}
		
		@Override
		public String str() {
			return "(";
		}

		@Override
		public TermType type() {
			return TermType.STRUCTURE;
		}

	};

	/**
	 * A placeholder term which is used during parsing to mark where a implicit bracket was opened.
	 * No STRUCTURE type terms will ever show up in the final expression.
	 */
	private static final MathTerm IMPLICIT_BRACKET_PLACEHOLDER = new MathTerm() {

		@Override
		public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {}

		@Override
		public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {}
		
		@Override
		public String str() {
			return "#";
		}

		@Override
		public TermType type() {
			return TermType.STRUCTURE;
		}

	};

	/**
	 * The pattern which matches  infix constant assignments.
	 * A (matched) matcher created with this pattern can safely be passed to {@link MathExpression#parseAndEvaluateAssignment(Matcher, MathContext)}
	 */
	public static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("(\\w+)(?:\\[(\\w+)\\])?(?:\\s+(\\w+)(?:\\[(\\w+)\\])?)?\\s*:=\\s*(.*)", Pattern.DOTALL);

	/**
	 * Attempts to match {@link MathExpression#ASSIGNMENT_PATTERN} on the input infix string and parse it as an constant assignment.
	 * The expression of the assignment will be evaluated using the {@link MathContext} parameter map, and the result will be registered as constant.
	 * @param infixStr The infix string to parse
	 * @param ctx The math context to use
	 * @return The expression which was evaluated and assigned to the constant
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 */
	public static MathExpression parseAndEvaluateAssignment(String infixStr, MathContext ctx) {
		
		Matcher m = ASSIGNMENT_PATTERN.matcher(infixStr);
		if (!m.find())
			throw new MathematicalExpressionException("not a valid assignment syntax: " + infixStr);
		
		return parseAndEvaluateAssignment(m, ctx);
	}

	/**
	 * Attempts to parse the match as an constant assignment.
	 * The expression of the assignment will be evaluated using the {@link MathContext} parameter map, and the result will be registered as constant.
	 * @param m The matcher, which has already been matched to the {@link MathExpression#ASSIGNMENT_PATTERN}
	 * @param ctx The math context to use
	 * @return The expression which was evaluated and assigned to the constant
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 * @throws MathematicalEvaluationException if the expression failed to evaluate into a concrete value for the assignment
	 */
	public static MathExpression parseAndEvaluateAssignment(Matcher m, MathContext ctx) {
		
		try {
			
			String varName1 = m.group(1);
			String deriveVar1 = m.group(2);
			
			String varName2 = m.group(3);
			String deriveVar2 = m.group(4);
			
			MathExpression expression = MathExpression.parseInfix(m.group(5), ctx);
			
			if (deriveVar1 != null || deriveVar2 != null) {
				if (!Objects.equals(deriveVar1, deriveVar2))
					throw new MathematicalExpressionException("computation of multiple partial derivatives not supported: " + m.group());
				var result = expression.evaluateAndDerive(ctx.varmap(), deriveVar1 == null ? deriveVar2 : deriveVar1);
				
				if (varName1 != null)
					ctx.register(new ConstantTerm(varName1, deriveVar1 != null ? result.derivative() : result.value()));
				if (varName2 != null)
					ctx.register(new ConstantTerm(varName2, deriveVar2 != null ? result.derivative() : result.value()));
			} else {
				var result = expression.evaluate(ctx.varmap());
				
				if (varName1 != null)
					ctx.register(new ConstantTerm(varName1, result));
				if (varName2 != null)
					ctx.register(new ConstantTerm(varName2, result));
			}
			
			return expression;
		
		} catch (MathematicalEvaluationException e) {
			throw new MathematicalEvaluationException("unable to evaluate expression for assignment: " + m.group(), e);
		} catch (MathematicalExpressionException e) {
			throw new MathematicalExpressionException("unable to parse and evaluate assignment: " + m.group(), e);
		}
		
	}
	
	/**
	 * Parses an infix notation expression with the supplied {@link MathContext} and constructs a new expression object which can be evaluated.
	 * @param infixStr The infix expression
	 * @param ctx The math context to use
	 * @return The newly created expression instance
	 * @throws MathematicalExpressionException if the expression is malformed, note that not all cases of ill-formed expressions are captured, some will only show up during evaluation
	 */
	public static MathExpression parseInfix(String infixStr, MathContext ctx) {

		List<MathTerm> postfix = new ArrayList<MathTerm>();
		Stack<MathTerm> operators = new Stack<MathTerm>();
		
		TermType type = null;
		int i0 = 0;
		for (int i1 = 0; i1 <= infixStr.length(); i1++) {
			
			char c = i1 == infixStr.length() ? 0 : infixStr.charAt(i1);
			if (Character.isWhitespace(c))
				continue;
			boolean endOfTerm = type != null && !type.test(c, false) || i1 == infixStr.length();
			
			if (endOfTerm) {
				
				if (type == null)
					break;
				
				String str = infixStr.substring(i0, i1).strip();
				switch (type) {
				
				case NUMERIC:
					try {
						// push numerical literal to output
						postfix.add(ctx.literal(Double.parseDouble(str)));
					} catch (NumberFormatException e) {
						throw new MathematicalExpressionException(infixStr, i1, "number has no valid format", e);
					}
					break;
				case LABEL:
					MathTerm term;
					if ((term = ctx.function(str)) != null)
						// push function or constant to output
						operators.push(term);
					else if ((term = ctx.constant(str)) != null)
						// add function to stack
						postfix.add(term);
					else
						// add function to stack
						postfix.add(ctx.variable(str));
					break;
				case OPERATOR:
					MathOperator op = ctx.operator(str);
					if (op == null)
						throw new MathematicalExpressionException(infixStr, i1, "invalid operator");
					// push higher precedence operators to output up to the first explicit or implicit bracket
					while (!operators.isEmpty() && operators.peek().type() != TermType.STRUCTURE && (((MathOperator) operators.peek()).precedence() > op.precedence() || (((MathOperator) operators.peek()).precedence() == op.precedence() && op.leftAssociative())))
						postfix.add(operators.pop());
					// add operator to stack
					operators.push(op);
					// add implicit bracket if needed
					if (op.implicitBracket())
						operators.add(IMPLICIT_BRACKET_PLACEHOLDER);
					break;
				case STRUCTURE:
					switch (str.charAt(0)) {
					case ',':
					case ':':
						try {
							// push all operators to output up to first explicit or implicit bracket
							while (/*!operators.isEmpty() &&*/ operators.peek().type() != TermType.STRUCTURE)
								postfix.add(operators.pop());
						} catch (EmptyStackException e) {
							// this exception is thrown under the assumption that there are no conditions in which multiple arguments before any operators or brackets are allowed.
							// examples for such conditions would be if the ? operator would first take its two cases and then the condition.
							throw new MathematicalExpressionException(infixStr, i1, "argument seperation outside brackets or implicit brackets");
						}
						break;
					case '(':
					case '[':
						// add explicit bracket to stack
						operators.add(BRACKET_PLACEHOLDER);
						break;
					case ')':
					case ']':
						try {
							// push all operators to output up to first explicit bracket (discarding/closing implicit brackets within)
							while (operators.peek() != BRACKET_PLACEHOLDER)
								if (operators.peek().type() != TermType.STRUCTURE)
									postfix.add(operators.pop());
								else
									operators.pop();
							// remove explicit bracket from stack
							operators.pop();
						} catch (EmptyStackException e) {
							throw new MathematicalExpressionException(infixStr, i1, "excess closing bracket");
						}
						// push function operator out if present immediately before the bracket
						if (operators.peek().type() == TermType.LABEL)
							postfix.add(operators.pop());
						break;
					}
				}
				
			}
			
			if ((endOfTerm || type == null) && i1 != infixStr.length()) {
				i0 = i1;
				TermType last = type;
				type = c == '-' && last != TermType.NUMERIC && last != TermType.LABEL ? TermType.NUMERIC : TermType.ofChar(c, true);
			}
			
		}
		
		// push all remaining operators to output (discarding/closing implicit brackets within)
		while (!operators.isEmpty())
			if (operators.peek().type() != TermType.STRUCTURE)
				postfix.add(operators.pop());
			else
				operators.pop();
		
		return new MathExpression(postfix);
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		for (MathTerm f : this.postfixTerms) {
			try {
				f.evaluate(evalstack, parameters);
			} catch (MathematicalEvaluationException e) {
				throw new MathematicalEvaluationException("unable to evaluate incomplete or malformed expression: " + str(), e);
			}
		}
	}

	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		for (MathTerm f : this.postfixTerms) {
			try {
				f.evaluateAndDerive(evalstack, diffstack, parameters, variable);
			} catch (MathematicalEvaluationException e) {
				throw new MathematicalEvaluationException("unable to evaluate incomplete or malformed expression: " + str(), e);
			}
		}
	}
	
	/**
	 * Evaluates this expression using an new and empty evaluation stack and the supplied parameters.
	 * @param parameters The parameters (variable values) for this evaluation
	 * @return The result of this expression for the given parameters
	 * @throws MathematicalEvaluationException If an exception during evaluation occurs, an example would be that this expression is an incomplete expression which can only be invoked as part of an another expression or a required parameters is missing
	 */
	public double evaluate(Map<String, Double> parameters) {
		Stack<Double> evalstack = new Stack<Double>();
		evaluate(evalstack, parameters);
		if (evalstack.size() != 1)
			throw new MathematicalEvaluationException("incomplete evaluation due to incomplete or malformed expression: " + str());
		return evalstack.pop();
	}
	
	/**
	 * Evaluates this expression using an new empty evaluation stack and no parameters.
	 * @return The result of this expression
	 * @throws MathematicalEvaluationException If an exception during evaluation occurs, an example would be that this expression is an incomplete expression which can only be invoked as part of an another expression or an required parameters is missing
	 */
	public double evaluate() {
		return evaluate(Collections.emptyMap());
	}

	/**
	 * Result of an evaluation and derivation of an expression combined as an pair.
	 */
	public static record ValueAndDerivative(double value, double derivative) {}
	
	/**
	 * Evaluates the results of this expression and its partial derivative in respect to the given variable.
	 * @param parameters The of parameters (variable values) for this evaluation
	 * @param variable The variable for which the partial derivative should be computed
	 */
	public ValueAndDerivative evaluateAndDerive(Map<String, Double> parameters, String variable) {
		Stack<Double> evalstack = new Stack<Double>();
		Stack<Double> diffstack = new Stack<Double>();
		evaluateAndDerive(evalstack, diffstack, parameters, variable);
		if (evalstack.size() != 1)
			throw new MathematicalEvaluationException("incomplete evaluation due to incomplete or malformed expression: " + str());
		if (diffstack.size() != evalstack.size())
			throw new IllegalStateException("evaluation stack and differentiation stack sizes differ, this indicates an implementation error in an mathematical function or operator");
		return new ValueAndDerivative(evalstack.pop(), diffstack.pop());
	}
	
	@Override
	public String str() {
		StringBuffer sb = new StringBuffer();
		for (var term : this.postfixTerms)
			sb.append(" ").append(term.str());
		return sb.toString();
	}

	@Override
	public TermType type() {
		return TermType.LABEL;
	}
	
	@Override
	public String toString() {
		return String.format("MathExpression{ %s }", str());
	}

}
