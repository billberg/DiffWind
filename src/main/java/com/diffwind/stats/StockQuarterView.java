package com.diffwind.stats;

import java.util.Calendar;
import java.util.Date;

import com.diffwind.util.DateUtil;

/**
 * TODO: EBIT
 * @author billberg
 *
 */
public class StockQuarterView {
    //类型: F-财报日, G-股本变更日, X-财报日且是股本变更日
	private char type = 'F';
	
    private String symbol;

    //财报日
    private Date reportDate;
    //财报披露日
    @Deprecated
    private Date piluDate;
    
    //财报日的年、季度
    private int reportYear;
    private int reportQuarter;
    
    
    /******** 盈利指标 ********/
    //主营业务收入
    private Double mainBizIncome = Double.NaN;
    
    //季报净利润
    private Double netprofit = Double.NaN;
    //季报EBIT粗估
    private Double EBIT = Double.NaN;
    //利润表科目-税金及附加
    private Double jingyingShui = Double.NaN;
    //利润表科目-所得税费用
    private Double suodeShui = Double.NaN;
    
    //上年净利润
    private Double netprofit1Y = Double.NaN;
	//上4个季度净利润
	private Double netprofit4Q = Double.NaN;
	//预期年净利润
	private Double netprofitExp = Double.NaN;// 与上年同期相比的年净利润估值
	
	// 上年ROE
	private Double weightedroe1Y = Double.NaN;
	//上4个季度ROE
	private Double weightedroe4Q = Double.NaN;
	//预期年ROE
	private Double weightedroeExp = Double.NaN;// 与上年同期相比的年净资产收益率估值
	
	//TODO
	//上年EBIT
    private Double EBIT1Y = Double.NaN;
	//上4个季度EBIT
	private Double EBIT4Q = Double.NaN;
	
	//股本
	private Double totalGuben = Double.NaN;
	
	//每股净资产=净资产/总股本
	private Double naps = Double.NaN;
	//每股收益=净利润/总股本
	//根据上年净利润计算
	private Double eps1Y = Double.NaN;
	//根据上4个季度净利润计算
	private Double eps4Q = Double.NaN;
	//根据预期年净利润计算
	private Double epsExp = Double.NaN;
	
	/******** 规模指标 ********/
	//总资产
	private Double totalAssets = Double.NaN;
	//净资产
	private Double totalShareEquity = Double.NaN;
	
	/******** 成长指标 ********/
	//净资产增长率（相比去年底）
	private Double netAssetsGrowthRate = Double.NaN;
	//总资产增长率（相比去年底）
	private Double totalAssetsGrowthRate = Double.NaN;
	//主营业务收入增长率（同比）
	private Double mainBizIncomeGrowthRate = Double.NaN;
	//净利润增长率（同比）
	
	/******** 股权结构 ********/
	//TODO 总股本
	
	
	public String toString() {
		return String.format("{piluDate: %s, reportDate: %s, type: %s}", DateUtil.yyyyMMdd10.get().format(piluDate),DateUtil.yyyyMMdd10.get().format(reportDate), type);
	}
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Date getReportDate() {
		return reportDate;
	}
	public void setReportDate(Date reportDate) {
		this.reportDate = reportDate;
		Calendar cal = Calendar.getInstance();
		cal.setTime(reportDate);
		reportYear = cal.get(Calendar.YEAR);
		reportQuarter = (cal.get(Calendar.MONTH) + 1)/3;
	}
	public Double getNetprofit1Y() {
		return netprofit1Y;
	}
	public void setNetprofit1Y(Double netprofit1y) {
		netprofit1Y = netprofit1y;
	}
	public Double getNetprofit4Q() {
		return netprofit4Q;
	}
	public void setNetprofit4Q(Double netprofit4q) {
		netprofit4Q = netprofit4q;
	}
	public Double getNetprofitExp() {
		return netprofitExp;
	}
	public void setNetprofitExp(Double netprofitExp) {
		this.netprofitExp = netprofitExp;
	}
	public Double getWeightedroe1Y() {
		return weightedroe1Y;
	}
	public void setWeightedroe1Y(Double weightedroe1y) {
		weightedroe1Y = weightedroe1y;
	}
	public Double getWeightedroe4Q() {
		return weightedroe4Q;
	}
	public void setWeightedroe4Q(Double weightedroe4q) {
		weightedroe4Q = weightedroe4q;
	}
	public Double getWeightedroeExp() {
		return weightedroeExp;
	}
	public void setWeightedroeExp(Double weightedroeExp) {
		this.weightedroeExp = weightedroeExp;
	}
	public Double getNaps() {
		return naps;
	}
	public void setNaps(Double naps) {
		this.naps = naps;
	}
	public Double getEps1Y() {
		return eps1Y;
	}
	public void setEps1Y(Double eps1y) {
		eps1Y = eps1y;
	}
	public Double getEps4Q() {
		return eps4Q;
	}
	public void setEps4Q(Double eps4q) {
		eps4Q = eps4q;
	}
	public Double getEpsExp() {
		return epsExp;
	}
	public void setEpsExp(Double epsExp) {
		this.epsExp = epsExp;
	}
	public Double getTotalAssets() {
		return totalAssets;
	}
	public void setTotalAssets(Double totalAssets) {
		this.totalAssets = totalAssets;
	}
	public Double getNetAssetsGrowthRate() {
		return netAssetsGrowthRate;
	}
	public void setNetAssetsGrowthRate(Double netAssetsGrowthRate) {
		this.netAssetsGrowthRate = netAssetsGrowthRate;
	}
	public Double getTotalAssetsGrowthRate() {
		return totalAssetsGrowthRate;
	}
	public void setTotalAssetsGrowthRate(Double totalAssetsGrowthRate) {
		this.totalAssetsGrowthRate = totalAssetsGrowthRate;
	}
	@Deprecated
	public Date getPiluDate() {
		return piluDate;
	}
	@Deprecated
	public void setPiluDate(Date piluDate) {
		this.piluDate = piluDate;
	}
	public Double getTotalShareEquity() {
		return totalShareEquity;
	}
	public void setTotalShareEquity(Double totalShareEquity) {
		this.totalShareEquity = totalShareEquity;
	}
	public char getType() {
		return type;
	}
	public void setType(char type) {
		this.type = type;
	}

	public int getReportYear() {
		return reportYear;
	}

	public void setReportYear(int reportYear) {
		this.reportYear = reportYear;
	}

	public int getReportQuarter() {
		return reportQuarter;
	}

	public void setReportQuarter(int reportQuarter) {
		this.reportQuarter = reportQuarter;
	}

	public Double getNetprofit() {
		return netprofit;
	}

	public void setNetprofit(Double netprofit) {
		this.netprofit = netprofit;
	}

	public Double getEBIT() {
		return EBIT;
	}

	public void setEBIT(Double eBITDA) {
		EBIT = eBITDA;
	}

	public Double getMainBizIncomeGrowthRate() {
		return mainBizIncomeGrowthRate;
	}

	public void setMainBizIncomeGrowthRate(Double mainBizIncomeGrowthRate) {
		this.mainBizIncomeGrowthRate = mainBizIncomeGrowthRate;
	}

	public Double getEBIT1Y() {
		return EBIT1Y;
	}

	public void setEBIT1Y(Double eBITDA1Y) {
		EBIT1Y = eBITDA1Y;
	}

	public Double getEBIT4Q() {
		return EBIT4Q;
	}

	public void setEBIT4Q(Double eBITDA4Q) {
		EBIT4Q = eBITDA4Q;
	}

	public Double getTotalGuben() {
		return totalGuben;
	}

	public void setTotalGuben(Double totalGuben) {
		this.totalGuben = totalGuben;
	}

	public Double getJingyingShui() {
		return jingyingShui;
	}

	public void setJingyingShui(Double jingyingShui) {
		this.jingyingShui = jingyingShui;
	}

	public Double getSuodeShui() {
		return suodeShui;
	}

	public void setSuodeShui(Double suodeShui) {
		this.suodeShui = suodeShui;
	}

	public Double getMainBizIncome() {
		return mainBizIncome;
	}

	public void setMainBizIncome(Double mainBizIncome) {
		this.mainBizIncome = mainBizIncome;
	}
	
	
}