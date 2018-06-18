package com.diffwind.stats;

import java.util.Date;
import java.util.List;

public class StockDayView {
    
    private String symbol;

    private Date date;

    private Double pb;

    private Double pe1Y;
    private Double pe4Q;
    private Double peExp;
    
    private Double MV2EBIT1Y;
    private Double MV2EBIT4Q;

    private Double zongshizhi;

    private Double liutongshizhi;
    
    //
    private Double close;
    
    //目前模型指标并未使用，该字段只用于图形拟合与相关性计算
    private Double fqClose;
    
    //季度报表
    private StockQuarterView quarterView;

    
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Double getPb() {
		return pb;
	}

	public void setPb(Double pb) {
		this.pb = pb;
	}

	public Double getPe1Y() {
		return pe1Y;
	}

	public void setPe1Y(Double pe1y) {
		pe1Y = pe1y;
	}

	public Double getPe4Q() {
		return pe4Q;
	}

	public void setPe4Q(Double pe4q) {
		pe4Q = pe4q;
	}

	public Double getPeExp() {
		return peExp;
	}

	public void setPeExp(Double peExp) {
		this.peExp = peExp;
	}

	public Double getZongshizhi() {
		return zongshizhi;
	}

	public void setZongshizhi(Double zongshizhi) {
		this.zongshizhi = zongshizhi;
	}

	public Double getLiutongshizhi() {
		return liutongshizhi;
	}

	public void setLiutongshizhi(Double liutongshizhi) {
		this.liutongshizhi = liutongshizhi;
	}

	public Double getClose() {
		return close;
	}

	public void setClose(Double close) {
		this.close = close;
	}
	

	public Double getFqClose() {
		return fqClose;
	}

	public void setFqClose(Double fqClose) {
		this.fqClose = fqClose;
	}

	public StockQuarterView getQuarterView() {
		return quarterView;
	}

	public void setQuarterView(StockQuarterView quarterView) {
		this.quarterView = quarterView;
	}

	public Double getMV2EBIT1Y() {
		return MV2EBIT1Y;
	}

	public void setMV2EBIT1Y(Double mV2EBIT1Y) {
		MV2EBIT1Y = mV2EBIT1Y;
	}

	public Double getMV2EBIT4Q() {
		return MV2EBIT4Q;
	}

	public void setMV2EBIT4Q(Double mV2EBIT4Q) {
		MV2EBIT4Q = mV2EBIT4Q;
	}

	
	
}