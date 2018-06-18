package com.diffwind.stats;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.diffwind.stats.MemStatsStockDay;
import com.diffwind.stats.StockDayViewWind;
import com.diffwind.stats.StockQuarterViewWind;
import com.diffwind.util.DateUtil;

public class MemStatsStockDayTest {

	@Test
	public void test() {
		//sz300317复权计算结果遗漏了2015-07-07
		//String symbol = "sz300317";
		String symbol = "sz000625";
		//String symbol = "sh600011";
		//String symbol = "sz002152";
		StockDayViewWind stockDayViewWind = MemStatsStockDay.stats(symbol);
		//测试
		//JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
		//System.out.print(JSON.toJSONString(stockDayViewWind,SerializerFeature.WriteDateUseDateFormat));
		
		String[] date = stockDayViewWind.dayViewHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getDate())).toArray(String[]::new);
		System.out.println("Date: " + Arrays.toString(date));
		double[] pe1Y = stockDayViewWind.dayViewHist.stream().mapToDouble(obj -> obj.getPe1Y()).toArray();
		System.out.println("PE1Y: " + Arrays.toString(pe1Y));
		double[] pe4Q = stockDayViewWind.dayViewHist.stream().mapToDouble(obj -> obj.getPe4Q()).toArray();
		System.out.println("PE4Q: " + Arrays.toString(pe4Q));
		
		
		String[] reportDate = stockDayViewWind.financeChgHistWind.financeChgHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println("Report Date: " + Arrays.toString(reportDate));
		
		double[] eps1Y = stockDayViewWind.financeChgHistWind.financeChgHist.stream().mapToDouble(obj -> obj.getEps1Y()).toArray();
		System.out.println("EPS1Y: " + Arrays.toString(eps1Y));
		
		
		double[] close = stockDayViewWind.dayViewHist.stream().mapToDouble(obj -> obj.getClose()).toArray();
		
		System.out.println(Arrays.toString(close));
		
		double[] fqClose = stockDayViewWind.dayViewHist.stream().mapToDouble(obj -> obj.getFqClose()).toArray();
		
		System.out.println(Arrays.toString(fqClose));
	}
	
	
	@Test
	public void test2() {
	
		String symbol = "sz000625";
		StockDayViewWind stockDayViewWind = MemStatsStockDay.stats(symbol);
		
		System.out.println(stockDayViewWind.financeChgHistWind);
	}
	
	
	@Test
	public void test3() {
		//String symbol = "sz000625";
		String symbol = "sz300255";
		
		//sz300255股本变更数据新浪与雪球不一致2016-08-16 雪球变动日为2016-08-17新浪变动日为2016-08-18
		
		StockDayViewWind stockDayViewWind = MemStatsStockDay.stats(symbol);
		StockQuarterViewWind financeQuarterHist = stockDayViewWind.financeChgHistWind;
		//测试
		//JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
		//System.out.print(JSON.toJSONString(financeQuarterHist.financeChgHist,SerializerFeature.WriteDateUseDateFormat));
	
		/*
		String[] reportDate = financeQuarterHist.financeChgHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println(Arrays.toString(reportDate));
		
		String[] reportQuarter = financeQuarterHist.quarterFinanceHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println(Arrays.toString(reportQuarter));
		
		String[] reportYear = financeQuarterHist.yearFinanceHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println(Arrays.toString(reportYear));
		*/
		System.out.print(financeQuarterHist);
		
	}
	
}
