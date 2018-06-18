package com.diffwind.stats;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.diffwind.stats.MemStatsStockQuarter;
import com.diffwind.stats.StockQuarterViewWind;
import com.diffwind.util.DateUtil;

public class MemStatsStockQuarterTest {

	@Test
	public void test() {
		//String symbol = "sz000625";
		//String symbol = "sz300255";
		String symbol = "sz000513";
		
		StockQuarterViewWind financeQuarterHist = MemStatsStockQuarter.stats(symbol);
		//测试
		//JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
		//System.out.print(JSON.toJSONString(financeQuarterHist.financeChgHist,SerializerFeature.WriteDateUseDateFormat));
	
		String[] reportDate = financeQuarterHist.financeChgHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println(Arrays.toString(reportDate));
		
		String[] reportQuarter = financeQuarterHist.quarterFinanceHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println(Arrays.toString(reportQuarter));
		
		String[] reportYear = financeQuarterHist.yearFinanceHist.stream().map(obj -> DateUtil.yyyyMMdd10.get().format(obj.getReportDate())).toArray(String[]::new);
		System.out.println(Arrays.toString(reportYear));
		
		double[] EBIT = financeQuarterHist.quarterFinanceHist.stream().mapToDouble(obj -> obj.getEBIT()).toArray();
		System.out.println(Arrays.toString(EBIT));
		
		double[] EBIT4Q = financeQuarterHist.quarterFinanceHist.stream().mapToDouble(obj -> obj.getEBIT4Q()).toArray();
		System.out.println(Arrays.toString(EBIT4Q));
	}
	
	@Test
	public void roundTest() {
		//以下结果不一样
		System.out.println(Math.round(401.5));
		System.out.println(Math.round(4.015*100));
		System.out.println(4.015*100);
		
		//以下结果不一样，double类型只要做运算，结果就可能不精确
		double   abc   =   4.5;   //4.025
		System.out.println(abc);
		System.out.println(Double.toString(abc));
		System.out.println(new  java.math.BigDecimal(abc).setScale(2,java.math.BigDecimal.ROUND_HALF_UP));
		System.out.println(new  java.math.BigDecimal("4.015").setScale(2,java.math.BigDecimal.ROUND_HALF_UP));
		
		

	}
}
