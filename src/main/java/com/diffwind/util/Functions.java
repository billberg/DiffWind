package com.diffwind.util;

import java.math.BigDecimal;
import java.util.Iterator;

public class Functions {

	//简单收益率
	//simR+1 = c1/c2
	public static double simR(double c1, double c2) {
		return (c1-c2)/c2;		
	}
	
	//对数收益率
	//logR = log(simR+1) = log(C2) - log(C1)
	//simR = exp(logR) - 1
	public static double logR(double c1, double c2) {
		return Math.log(c1/c2);		
	}
	
	public static double simR2logR(double simR) {
		return Math.log(simR+1);
	}
	
	public static double logR2simR(double logR) {
		return Math.pow(Math.E, logR) - 1;
	}
	
	//根据长窗口收益计算微分窗口收益
	public static double calcDiffWindReturn(int longW, double longWChg, int shortW) {
		//先计算日对数收益，再计算shortW窗口收益
		double day_r = Functions.simR2logR(longWChg)/longW;
		double shortW_r = shortW*day_r;
		double shortW_R = Functions.logR2simR(shortW_r);
		return 100*shortW_R;
	}
		
	/**
	 * 时间序列（倒序）对数收益率
	 * 更新: 添加最后一个收益率为0，使收益率序列长度与原序列长度一致
	 * TODO: 回滚上次的更新
	 * @param ts
	 * @return
	 */
	public static double[] logR(double[] ts, int order) {
		double[] logr = new double[ts.length];
		if (order == -1) {
			logr[ts.length-1] = 0;
			for (int i = 0; i < ts.length-1; i++)
				logr[i] = Math.log(ts[i]/ts[i+1]);
		} else {
			throw new RuntimeException("未实现");
		}
			
		return logr;		
	}
	
	
	/**
	 * 时间序列（倒序）简单收益率
	 * 更新: 添加最后一个收益率为0，使收益率序列长度与原序列长度一致
	 * TODO: 回滚上次的更新
	 * @param ts
	 * @return
	 */
	public static double[] simR(double[] ts, int order) {
		double[] simr = new double[ts.length];
		if (order == -1) {
			simr[ts.length-1] = 0;
			for (int i = 0; i < ts.length-1; i++)
				simr[i] = ts[i]/ts[i+1] - 1;
		} else {
			throw new RuntimeException("未实现");
		}
			
		return simr;		
	}
	
	
	public static Double[] simR(Double[] ts, int order) {
		Double[] simr = new Double[ts.length];
		if (order == -1) {
			simr[ts.length-1] = 0d;
			for (int i = 0; i < ts.length-1; i++) {
				if (ts[i] == null || ts[i+1] == null)
					break;
				
				simr[i] = ts[i]/ts[i+1] - 1;
			}
		} else {
			throw new RuntimeException("未实现");
		}
			
		return simr;		
	}
	
	public static double[] multiply(double[] ts, double d) {
		double[] result = new double[ts.length];
		for (int i = 0; i < ts.length; i++)
			result[i] = ts[i]*d;
			
		return result;		
	}
	
	public static Double[] multiply(Double[] ts, double d) {
		Double[] result = new Double[ts.length];
		for (int i = 0; i < ts.length; i++) {
			if (ts[i] == null)
				break;
			
			result[i] = ts[i]*d;
		}
			
		return result;		
	}
	
	public static double[] cumsum(double[] ts, int order) {
		double[] result = new double[ts.length];
		double cumsum = 0;
		if (order == -1) {
			for (int i = ts.length-1; i >= 0; i--) {
				cumsum = cumsum + ts[i];
				result[i] = cumsum;
			}
		} else {
			throw new RuntimeException("未实现");
		}
			
		return result;		
	}
	
	//必须保证不丢失信息
	public static double[] cumsub(double[] ts, int order) {
		double[] result = new double[ts.length];
		double cumsub = 0;
		if (order == 1) {
			for (int i = 0; i < ts.length; i++) {
				cumsub = cumsub - ts[i];
				result[i] = cumsub;
			}
		} else {
			throw new RuntimeException("未实现");
		}
			
		return result;		
	}
	
	/**
	 * 从0开始求累积和
	 * @param ts
	 * @param order
	 * @return 比原序列长度加1
	 */
	public static double[] cumsum0(double[] ts, int order) {
		double[] result = new double[ts.length+1];
		double cumsum = 0;
		if (order == -1) {
			result[ts.length] = 0;
			for (int i = ts.length-1; i >= 0; i--) {
				cumsum = cumsum + ts[i];
				result[i] = cumsum;
			}
		} else {
			throw new RuntimeException("未实现");
		}
			
		return result;		
	}
	
	/**
	 * 从0开始求累积差
	 * @param ts
	 * @param order
	 * @return
	 */
	public static double[] cumsub0(double[] ts, int order) {
		double[] result = new double[ts.length+1];
		double cumsub = 0;
		if (order == 1) {
			result[0] = 0;
			for (int i = 0; i < ts.length; i++) {
				cumsub = cumsub - ts[i];
				result[i+1] = cumsub;
			}
		} else {
			throw new RuntimeException("未实现");
		}
			
		return result;		
	}
	
	
	/*
	public static double[] logR(double[] ts) {
		double[] r = new double[ts.length-1];
		for (int i = 0; i < ts.length-1; i++)
			r[i] = Math.log(ts[i]/ts[i+1]);
			
		return r;		
	}
	*/
	
	/**
	 * 查找最大值所在序号
	 * 
	 * @param arr
	 * @param startIndex 数组开始下标，包含
	 * @param endIndex 数组结束下标，不包含
	 * @return
	 */
	public static int whichMax(double[] arr, int startIndex, int endIndex) {
		//assert (startIndex >= 0 && startIndex < endIndex && endIndex <= arr.length);
		if (!(startIndex >= 0 && startIndex < endIndex && endIndex <= arr.length)) {
			return -1;
		}
		
		int pos = startIndex;
		for (int i = startIndex+1; i < endIndex; i++) {
			if (arr[i] > arr[pos]) {
				pos = i;
			} 
		}
		
		return pos;
	}
	
	/**
	 * 查找最小值所在序号
	 * 
	 * @param arr
	 * @param startIndex 数组开始下标，包含
	 * @param endIndex 数组结束下标，不包含
	 * @return
	 */
	public static int whichMin(double[] arr, int startIndex, int endIndex) {
		//assert (startIndex >= 0 && startIndex < endIndex && endIndex <= arr.length);
		if (!(startIndex >= 0 && startIndex < endIndex && endIndex <= arr.length)) {
			return -1;
		}
		
		int pos = startIndex;
		for (int i = startIndex+1; i < endIndex; i++) {
			if (arr[i] < arr[pos]) {
				pos = i;
			} 
		}
		
		return pos;
	}
	
	public static double min(double[] arr) {
		if (checkHasNa(arr)) {
			return Double.NaN;
		}
		
		double minValue = arr[0];
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] < minValue) {
				minValue = arr[i];
			} 
		}
		
		return minValue;
	}
	
	public static boolean checkHasNa(double[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (Double.isNaN(arr[i]))
				return true;
		}
		
		return false;
	}
	
	public static double round(double d) {
		if (Double.isNaN(d) || Double.isInfinite(d))
			return d;
		
		return new  BigDecimal(Double.toString(d)).setScale(2,java.math.BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	public static double[] round(double[] d) {
		double[] rd = new double[d.length];
		for (int i = 0; i < d.length; i++) {
			rd[i] = round(d[i]);
		}
		
		return rd;
	}
	
	public static boolean in(int x, int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (x == arr[i])
				return true;
		}
		
		return false;
	}
	
}
