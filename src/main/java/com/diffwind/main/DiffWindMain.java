package com.diffwind.main;

import org.apache.log4j.Logger;

import com.diffwind.data.SinaHangyeDataRobot;
import com.diffwind.data.TdxDataDecoder;
import com.diffwind.data.XueqiuStockFinancialStatementRobot;
import com.diffwind.data.XueqiuStockFinanceDataRobot;
import com.diffwind.data.XueqiuStockShareschgDataRobot;
import com.diffwind.stats.DataHandler;
import com.diffwind.stats.filters.MingxingCasesFilter;

/**
 * 执行筛选模型，筛选规则参考{@link FilterRules}
 * 
 * @author zhangjz
 *
 */
public class DiffWindMain {

private static Logger logger = Logger.getLogger(DataHandler.class);
	
	public static void main(String[] args) {

		logger.info("-------- 数据处理开始 --------");
		
		//股票最新列表，名字或许更新，如st
		//［新浪］股票行业分类（全量，按月更新-可选）
		//SinaHangyeDataRobot.updateSinaZjhhangye();
		
		//［雪球］主要财务指标历史数据（增量，按月更新）
		XueqiuStockFinanceDataRobot.downloadFinanceData(true);
		//［雪球］股本结构历史数据（增量，按月更新）
		XueqiuStockShareschgDataRobot.downloadShareschgData(true);
		//［雪球］财务报表历史数据（全量）
		XueqiuStockFinancialStatementRobot.downloadIncomeStatementData(false);
		
		//［通达信］日线数据（增量，分区表，按日更新最近5年）
		TdxDataDecoder.start(2016,2020);

		
		logger.info("-------- 数据处理结束 --------");
		
		logger.info("-------- 筛选开始 -------- ");

		//10年统计筛选
		MingxingCasesFilter.filterByY(10, true);
		//5年统计筛选
		MingxingCasesFilter.filterByY(5, true);

		logger.info("-------- 筛选结束 -------- ");

	}
}
