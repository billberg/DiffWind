package com.diffwind.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diffwind.util.DateUtil;

/**
 * @20180128 更新：
 * 财报历史记录按报表日排序，不再使用披露日字段。股本变更记录按股本变动日期排序。
 * 
 * @author billberg
 *
 */
public class StockQuarterViewWind {

	public final String symbol;
	//(倒序)时间序列
	//财报历史记录（季度），根据披露日排序，假设披露日排序不会打乱报表日排序，如造成报表日乱序，应抛出异常（不再使用披露日）
	//@20180128 财报历史记录（季度），根据财报日排序
	public final List<StockQuarterView> quarterFinanceHist = new ArrayList<StockQuarterView>();
	//财报历史记录（年报）
	public final List<StockQuarterView> yearFinanceHist = new ArrayList<StockQuarterView>();
	//股本变更历史记录（如果财报日有股本变更，则同时包含财报日）
	public final List<StockQuarterView> sharesChgHist = new ArrayList<StockQuarterView>();
	
	//覆盖股本变更和财报历史记录，根据财报日和股本变动日排序(倒序)
	//TODO: 股本变更日与财报日重叠的情况，合并为财报日，案例: sz300255
	public final List<StockQuarterView> financeChgHist = new ArrayList<StockQuarterView>();
	//日期索引
	public final Map<Date,StockQuarterView> lookupDate = new HashMap<Date,StockQuarterView>();
	
	public StockQuarterViewWind(String symbol) {
		this.symbol = symbol;
	}
	
	public StockQuarterView at(Date d) {
		return lookupDate.get(d);
	}
	
	
	public void add_deprecated(StockQuarterView quarterView) {
		if (financeChgHist.size() > 0 && 
				(quarterView.getPiluDate().compareTo(financeChgHist.get(financeChgHist.size()-1).getPiluDate()) > 0
				/*|| quarterView.getReportDate().compareTo(financeChgHist.get(financeChgHist.size()-1).getReportDate()) >= 0*/))
			throw new RuntimeException(String.format("股本变更或财务历史数据时间乱序: %s > %s or %s >= %s", 
					DateUtil.yyyyMMdd10.get().format(quarterView.getPiluDate()), DateUtil.yyyyMMdd10.get().format(financeChgHist.get(financeChgHist.size()-1).getPiluDate()),
					DateUtil.yyyyMMdd10.get().format(quarterView.getReportDate()), DateUtil.yyyyMMdd10.get().format(financeChgHist.get(financeChgHist.size()-1).getReportDate()) ) );
		
		financeChgHist.add(quarterView);
		if ('F' == quarterView.getType()) {
			quarterFinanceHist.add(quarterView);
			if (quarterView.getReportQuarter() == 4) {
				yearFinanceHist.add(quarterView);
			}
		} else if ('G' == quarterView.getType()) {
			sharesChgHist.add(quarterView);
		} 
		
		lookupDate.put(quarterView.getReportDate(), quarterView);
	}
	
	
	public void add(StockQuarterView quarterView) {
		
		financeChgHist.add(quarterView);
		
		if ('F' == quarterView.getType() || 'X' == quarterView.getType()) {
			quarterFinanceHist.add(quarterView);
			if (quarterView.getReportQuarter() == 4) {
				yearFinanceHist.add(quarterView);
			}
		} else if ('G' == quarterView.getType() || 'X' == quarterView.getType()) {
			sharesChgHist.add(quarterView);
		} 
		
		lookupDate.put(quarterView.getReportDate(), quarterView);
	}
	
	public int size() {
		return financeChgHist.size();
	}
	
	//TODO: 提供该方法不好，待完善
	public StockQuarterView get(int i) {
		return financeChgHist.get(i);
	}
	
	public void addAll(List<StockQuarterView> quarterViewHist) {
		for (StockQuarterView qv : quarterViewHist) {
			add(qv);
		}
	}
	
	
	//TODO
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("-------- %s --------\n", symbol) );
		
		String[] reportDate = quarterFinanceHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getPiluDate()) +"@"+ DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		sb.append("财报历史记录: ").append(Arrays.toString(reportDate)).append("\n");
		reportDate = sharesChgHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		sb.append("股本变更历史记录: ").append(Arrays.toString(reportDate)).append("\n");
		reportDate = financeChgHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate()) + obj.getType()).toArray(String[]::new);
		sb.append("股本变更和财报历史记录: ").append(Arrays.toString(reportDate)).append("\n");
		
		sb.append(String.format("-------- %s --------\n", symbol) );
		
		return sb.toString();
	}
	
}
