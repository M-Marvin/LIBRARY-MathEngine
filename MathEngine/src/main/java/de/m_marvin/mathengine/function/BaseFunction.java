package de.m_marvin.mathengine.function;

import de.m_marvin.mathengine.term.MathTerm;

/**
 * The abstract base class of all functions which are not obtained trough parsing an infix string.
 * This includes all functions defined in code, such as sin() sig() abs() etc.
 */
public abstract class BaseFunction implements MathTerm {

	private final String name;
	
	/**
	 * @param name The name for the function
	 */
	public BaseFunction(String name) {
		this.name = name;
	}
	
	@Override
	public String str() {
		return name + "()";
	}

	@Override
	public TermType type() {
		return TermType.LABEL;
	}
	
	@Override
	public String toString() {
		return "BaseFunction{ " + str() + " }";
	}
	
}
