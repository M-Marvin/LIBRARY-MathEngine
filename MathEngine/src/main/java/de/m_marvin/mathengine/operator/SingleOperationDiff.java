package de.m_marvin.mathengine.operator;

/**
 * Helper interface to define the derivative of single mathematical operators
 */
@FunctionalInterface
public interface SingleOperationDiff {
	public double apply(double x, double xd);
}
