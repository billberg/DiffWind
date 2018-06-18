package com.diffwind.stats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockDayViewWind {

	public final String symbol;
	//(倒序)时间序列
	public final List<StockDayView> dayViewHist = new ArrayList<StockDayView>();
	//public final Map<Date,StockDayView> lookupDate = new HashMap<Date,StockDayView>();
	public final Map<Date,Integer> dateIndex = new HashMap<Date,Integer>();
	
	//股本变更和财报历史记录
	public StockQuarterViewWind financeChgHistWind = null;
	
	public StockDayViewWind(String symbol) {
		this.symbol = symbol;
	}
	
	public void add(StockDayView dayView) {
		dayViewHist.add(dayView);
		dateIndex.put(dayView.getDate(), dayViewHist.size()-1);
	}
	
	public StockDayView at(Date d) {
		Integer index = dateIndex.get(d);
		return dayViewHist.get(index);
	}
	
	public int size() {
		return dayViewHist.size();
	}
	
	//TODO: 提供该方法不好，待完善
	public StockDayView get(int i) {
		return dayViewHist.get(i);
	}
	
	/**
	 * fromDate < toDate
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<StockDayView> rangeOf(Date fromDate, Date toDate) {
		Integer fromIndex = -1, toIndex = -1;
		if (toDate.after(dayViewHist.get(0).getDate())) {
			fromIndex = 0;
		}
		if (fromDate.before(dayViewHist.get(dayViewHist.size()-1).getDate())) {
			toIndex = dayViewHist.size();
		} 
		
		if (toDate.before(dayViewHist.get(dayViewHist.size()-1).getDate()) 
				|| fromDate.after(dayViewHist.get(0).getDate()) 
				|| fromDate.after(toDate) ) {
			return null;
		}
		
		//
		Calendar cal = Calendar.getInstance();
		cal.setTime(toDate);
		while (dateIndex.get(toDate) == null && toDate.after(fromDate)) {
			cal.add(Calendar.DAY_OF_MONTH, -1);
			toDate = cal.getTime();
		}
		if (dateIndex.get(toDate) == null) {
			return null;
		}
		
		fromIndex = dateIndex.get(toDate);
		
		//
		cal.setTime(fromDate);
		while (dateIndex.get(fromDate) == null && fromDate.before(toDate)) {
			cal.add(Calendar.DAY_OF_MONTH, 1);
			fromDate = cal.getTime();
		}
		if (dateIndex.get(fromDate) == null) {
			return null;
		}
		
		toIndex = dateIndex.get(fromDate) + 1;
		
		return dayViewHist.subList(fromIndex, toIndex);
	}
	
	
	public List<StockDayView> rangeOf(int w) {
		if (w <= dayViewHist.size()) {
			return dayViewHist.subList(0, w);
		} else {
			return dayViewHist;
		}
	}
	

	
	/**
	 * 时间序列的指标矩阵
	 * @return
	 */
	public Double[][] toMatrix() {
		//每个指标一列，使用不是太方便
		Double[][] matrix = dayViewHist.stream().map(obj -> new Double[]{obj.getPb(),obj.getPe1Y()}).toArray(Double[][]::new);
		return matrix;

	}
	
}
