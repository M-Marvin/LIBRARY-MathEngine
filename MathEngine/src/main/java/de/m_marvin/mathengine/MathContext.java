package de.m_marvin.mathengine;

import java.util.Collection;
import java.util.Map;

import de.m_marvin.mathengine.expression.MathOperator;
import de.m_marvin.mathengine.function.BaseFunction;
import de.m_marvin.mathengine.function.MathFunction;
import de.m_marvin.mathengine.term.ConstantTerm;
import de.m_marvin.mathengine.term.MathTerm;

/**
 * A math context provides information about the available functions, constants and variable values during the parsing of infix notations.
 * It also collects newly defined functions, constants and variables during evaluation.
 * A math context should be created for every independent parsing operation which, but can be reused for further operations if functions and constants should persist and be available in the next operation.
 */
public interface MathContext {
	
	/**
	 * Returns the map to read variable values from.
	 * @return the map instance assigned to this context
	 */
	public Map<String, Double> varmap();
	
	/**
	 * Creates a term for an numeric literal value.
	 * @param value The numeric value of the term
	 * @return The term representing the numeric value
	 */
	public MathTerm literal(double value);
	
	/**
	 * Creates a term for an variable.
	 * @param str The name of the variable
	 * @return The term representing the variable
	 */
	public MathTerm variable(String str);
	
	/**
	 * Returns which variables where used during the last parsing operation.
	 * @return a list of all variables which where mentioned in the last parsed expression
	 */
	public Collection<String> variablesUsed();
	
	/**
	 * Clear the list of used variables, this should be called before a expression is parsed for which the required variables should be determined.
	 */
	public void clearVariablesUsed();

	/**
	 * Register a new {@link MathFunction}, {@link ConstantTerm}, {@link BaseFunction} or {@link MathOperator} for this context.
	 * If an object with the same name already exists, it will be replaced.
	 * The name is computed using {@link MathTerm#str()}
	 * @param object The object to register
	 */
	public void register(MathTerm object);
	
	/**
	 * Returns the term for the requested constant.
	 * @param str The name of the constant
	 * @return The term representing the constant or null if the constant is not found within this context
	 */
	public MathTerm constant(String str);
	
	/**
	 * Returns the term for the requested function.
	 * @param str The name of the function
	 * @return The term representing the function or null if the function is not found within this context
	 */
	public MathTerm function(String str);
	
	/**
	 * Returns the term for the operator.
	 * @param str The name of the operator
	 * @return The term representing the operator or null if the operator is not found within this context
	 */
	public MathOperator operator(String str);
	
}
