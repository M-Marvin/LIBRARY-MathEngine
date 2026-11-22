package de.m_marvin.mathengine.term;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import de.m_marvin.mathengine.MathematicalEvaluationException;

/**
 * A variable which's value is determined by the parameter map used during evaluation.
 */
public class VariableTerm implements MathTerm {

	// TODO improve caching ? maybe remove it ?
	private final static Map<String, VariableTerm> termCache = new HashMap<String, VariableTerm>();

	/**
	 * Returns a variable term for the supplied variable name
	 * @param name The name for the variable
	 * @return the variable term representing that variable
	 */
	public static VariableTerm valueOf(String name) {
		Objects.requireNonNull(name, "name");
		if (!name.matches("\\w+")) // TODO check regex
			throw new IllegalArgumentException("illegal variable name");
		VariableTerm term = termCache.get(name);
		if (term == null) {
			term = new VariableTerm(name);
			termCache.put(name, term);
		}
		return term;
	}

	private final String name;
	
	private VariableTerm(String name) {
		this.name = name;
	}

	@Override
	public TermType type() {
		return TermType.LABEL;
	}
	
	@Override
	public String str() {
		return this.name;
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		Double d = parameters.get(this.name);
		if (d == null)
			throw new MathematicalEvaluationException("parameter not defined: " + this.name);
		evalstack.add(d);
	}
	
	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		evaluate(evalstack, parameters);
		diffstack.add(this.name.equals(variable) ? 1.0 : 0.0);
	}
	
	@Override
	public String toString() {
		return "Variable{ " + str() + " }";
	}
	
}
