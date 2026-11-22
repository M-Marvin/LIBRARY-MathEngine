package de.m_marvin.mathengine;

import de.m_marvin.mathengine.expression.MathExpression;

public class MathTest {
	
	public static void main(String[] args) {
		
		String infix = "VAL:=1+1;VAL*2";
		
		MathParser parser = MathParser.standard();
		
		MathExpression expr = parser.parseInfixList(infix);
		
		System.out.println(expr);
		
		if (expr != null) {
			double result = expr.evaluate();
			System.out.println(" = " + result);
			
		}
		
	}
	
}
