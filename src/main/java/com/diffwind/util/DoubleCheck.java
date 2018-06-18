package com.diffwind.util;

public class DoubleCheck {
	public static boolean checkHasNa(Double... args) {
		for (Double d : args) {
			if (d == null || d.isNaN())
				return true;
		}
		
		return false;
	}
	
	public static boolean checkPositive(Double... args) {
		for (Double d : args) {
			if (d == null || d < 0d)
				return false;
		}
		
		return true;
	}
	
	
	public static Double ifNull2NaN(Double d) {
		if (d == null) 
			return Double.NaN;
		
		return d;
	}
	

}
