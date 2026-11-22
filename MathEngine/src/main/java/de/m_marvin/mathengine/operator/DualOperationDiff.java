package de.m_marvin.mathengine.operator;

/**
 * Helper interface to define the derivative of dual mathematical operators
 */
@FunctionalInterface
public interface DualOperationDiff {
	public double apply(double B, double Bd, double A, double Ad);
}
