package com.diffwind.stats;

import org.apache.log4j.Logger;

import com.diffwind.data.SinaHangyeDataRobot;
import com.diffwind.data.TdxDataDecoder;
import com.diffwind.data.XueqiuStockFinancialStatementRobot;
import com.diffwind.data.XueqiuStockFinanceDataRobot;
import com.diffwind.data.XueqiuStockShareschgDataRobot;

/**
 * 数据处理
 * 数据更新尽量改成增量，必要时使用分区表存历史数据
 */
public class DataHandler {

	private static Logger logger = Logger.getLogger(DataHandler.class);
	
	public static void main(String[] args) {

		logger.info("-------- 数据处理开始 --------");
		
		//股票最新列表，名字或许更新，如st
		//［新浪］股票行业分类（全量，按月更新）
		SinaHangyeDataRobot.updateSinaZjhhangye();
		
		//［雪球］主要财务指标历史数据（增量，按月更新）
		XueqiuStockFinanceDataRobot.downloadFinanceData(true);
		//［雪球］股本结构历史数据（增量，按月更新）
		XueqiuStockShareschgDataRobot.downloadShareschgData(true);
		//［雪球］财务报表历史数据
		XueqiuStockFinancialStatementRobot.downloadIncomeStatementData(false);
		
		//［通达信］日线数据（增量，分区表，按日更新最近5年）
		TdxDataDecoder.start(2016,2020);

		
		logger.info("-------- 数据处理结束 --------");

	}
}
