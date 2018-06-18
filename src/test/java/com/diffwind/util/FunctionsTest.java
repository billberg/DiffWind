package com.diffwind.util;

import java.util.Arrays;

import org.junit.Test;

import com.diffwind.util.Functions;

public class FunctionsTest {

	@Test
	public void test() {
		//312	445.74	11.49
		int days = 312;
		double R = 4.4574;
		double r = Functions.simR2logR(R);
		double day_r = r/days;
		double day_R = Functions.logR2simR(day_r);
		
		double R2 = Math.pow((1+day_R), days) - 1;
		
		System.out.println(r);
		System.out.println(day_r);
		System.out.println(day_R);
		System.out.println(R2);
		
		double days20_R = Functions.calcDiffWindReturn(days, R, 20);
		System.out.println(days20_R);
	}
	
	
	@Test
	public void test2() {
		double[] ts = {10, 11, 11.5, 11.2, 12};
		
		double[] cumsum0 = Functions.cumsum0(ts, -1);
		
		double[] cumsub0 = Functions.cumsub0(ts, 1);
		
		System.out.println(Arrays.toString(cumsum0));
		System.out.println(Arrays.toString(cumsub0));
	}
}
