package de.m_marvin.mathengine.operator;

/**
 * Helper interface to define single mathematical operators
 */
@FunctionalInterface
public interface SingleOperation {
	public double apply(double x);
}
