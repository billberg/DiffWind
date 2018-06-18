package com.diffwind.util;

/**
 * 风险的度量方法，应更准确
 * @author billberg
 *
 */
public class RiskProb {

	//
	@Deprecated
	public static double[] evaRiskProb(double[] ts, double maxRiskProb) {
		int[] order = Algorithm.bubbleSortAscending(ts);
		int pos = 0;
		for (int i = 0; i < order.length; i++)
			if (order[i] == 0) {
				pos = i;
				break;
			}
		
		//当前风险概率水平
		double riskProb = (pos+1)/(double)order.length;
		//满足风险条件的参考值
		double riskMark = 0d;
		int idx = (int)(maxRiskProb*ts.length) - 1;
		if (idx >= 0)
			riskMark = ts[order[idx]];
		
		return new double[]{riskProb,riskMark};
				
	}
	
	//剔除ts中的负值，假定ts[0]为正值
	@Deprecated
	public static double[] evaRiskProb_v2(double[] ts, double maxRiskProb) {
		int[] order = Algorithm.bubbleSortAscending(ts);
		int negativeCount = 0;
		int pos = 0;
		for (int i = 0; i < order.length; i++) {
			if (ts[order[i]] < 0)
				negativeCount++;
			
			if (order[i] == 0) {
				pos = i;
				break;
			}
		}
		
		//当前风险概率水平
		double riskProb = (pos+1-negativeCount)/(double)(order.length-negativeCount);
		//满足风险条件的水位
		double riskMark = 0d;
		int idx = (int)(maxRiskProb*(ts.length-negativeCount)) - 1;
		if (idx >= 0)
			riskMark = ts[order[idx+negativeCount]];
		
		return new double[]{riskProb,riskMark};
				
	}
	
	
	/**
	 * 计算PE, PB等风险指标的风险水位
	 * @param ts
	 * @param x
	 * @return
	 */
	public static double calcRiskProb(double[] ts, double x) {
		double riskProb = Double.NaN;
		int pos = 0;
		for (int i = 0; i < ts.length; i++)
			if (ts[i] < x) {
				pos++;
			}
		
		//当前风险概率水平
		riskProb = (pos+1)/(double)ts.length;
	
		return riskProb;
				
	}
}
