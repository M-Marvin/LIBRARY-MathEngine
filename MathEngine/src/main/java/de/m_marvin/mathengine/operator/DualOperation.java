package de.m_marvin.mathengine.operator;

/**
 * Helper interface to define dual mathematical operators
 */
@FunctionalInterface
public interface DualOperation {
	public double apply(double B, double A);
}
