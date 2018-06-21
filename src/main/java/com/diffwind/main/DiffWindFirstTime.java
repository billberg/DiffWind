package com.diffwind.main;

import org.apache.log4j.Logger;

import com.diffwind.data.SinaHangyeDataRobot;
import com.diffwind.data.TdxDataDecoder;
//import com.diffwind.data.XueqiuStockFinancialStatementRobot;
import com.diffwind.data.XueqiuStockFinanceDataRobot;
import com.diffwind.data.XueqiuStockShareschgDataRobot;
import com.diffwind.stats.DataHandler;

/**
 * DiffWind初次执行时需准备历史数据，初始化数据库
 * ［通达信］日线数据说明：
 * 作者使用的通达信软件客户端为Windows版本"招商证券-智远理财"客户端，通过"数据管理 > 盘后数据下载"下载日线数据。
 * 日线数据的存储路经为：C:/zd_zszq/vipdoc/ （C:/zd_zszq为软件安装路径）
 * 
 * @author billberg
 *
 */
public class DiffWindFirstTime {

private static Logger logger = Logger.getLogger(DataHandler.class);
	
	public static void main(String[] args) {

		logger.info("-------- 准备历史数据开始 --------");
		
		//股票最新列表，名字或许更新，如st
		//［新浪］股票行业分类（全量，按月更新）
		SinaHangyeDataRobot.updateSinaZjhhangye();
				
		//［雪球］主要财务指标历史数据（全量，下载20年数据）
		XueqiuStockFinanceDataRobot.downloadFinanceData(false);
		//［雪球］股本结构历史数据（全量，下载20年数据）
		XueqiuStockShareschgDataRobot.downloadShareschgData(false);
		//［雪球］财务报表历史数据（全量）
		//XueqiuStockFinancialStatementRobot.downloadIncomeStatementData(false);
				
		//［通达信］日线数据（全量，导入通达信盘后数据下载[2001-2015] 15年历史数据）
		TdxDataDecoder.start(2001, 2005);
		TdxDataDecoder.start(2006, 2010);
		TdxDataDecoder.start(2011, 2015);
		
		logger.info("-------- 准备历史数据结束 --------");

	}
}
