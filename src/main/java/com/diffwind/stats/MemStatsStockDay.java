package com.diffwind.stats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.diffwind.dao.mapper.TdxStockDayMapper;
import com.diffwind.dao.mapper.XueqiuStockShareschgMapper;
import com.diffwind.dao.model.TdxStockDay;
import com.diffwind.dao.model.XueqiuStockShareschg;
import com.diffwind.util.DateUtil;
import com.diffwind.util.DoubleCheck;

/**
 * 日统计，内存统计结果不持久化
 * 
 * @author billberg
 *
 */
public class MemStatsStockDay {

	private static Logger logger = Logger.getLogger(MemStatsStockDay.class);

	private static SqlSessionFactory sqlSessionFactory = null;
	private static SqlSessionFactory batchSqlSessionFactory = null;

	private static AtomicInteger finishedNum = new AtomicInteger();

	static {
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),
					"simpleds");
			batchSqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),
					"batchds");

		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	/**
	 * 指标计算: PE, PB
	 * 
	 * @param symbol
	 */
	public static StockDayViewWind stats(String symbol) {
		SqlSession sqlSession = null;
		SqlSession batchSqlSession = null;

		try {
			sqlSession = sqlSessionFactory.openSession(true);
			batchSqlSession = batchSqlSessionFactory.openSession(ExecutorType.BATCH, false);

			//XueqiuStockFinanceMapper xueqiuFinanceMapper = sqlSession.getMapper(XueqiuStockFinanceMapper.class);

			XueqiuStockShareschgMapper xueqiuShareschgMapper = sqlSession.getMapper(XueqiuStockShareschgMapper.class);

			// XueqiuStockDayMapper xueqiuStockDayMapper =
			// sqlSession.getMapper(XueqiuStockDayMapper.class);
			TdxStockDayMapper tdxStockDayMapper = sqlSession.getMapper(TdxStockDayMapper.class);

			//XueqiuStockDayFqMapper stockDayFMapper = sqlSession.getMapper(XueqiuStockDayFqMapper.class);

			//todo
			//List<XueqiuStockFinance> financeRecords = xueqiuFinanceMapper.selectBySymbol(symbol);

			List<XueqiuStockShareschg> sharesChgHist = xueqiuShareschgMapper.selectBySymbol(symbol);

			//TODO: 将XueqiuStockFinance必要指标添加到StockQuarterView，不再使用XueqiuStockFinance
			StockQuarterViewWind stkQuarterViewWind = MemStatsStockQuarter.stats(symbol);
			//测试
			//String[] dates = quarterViewHist.quarterViewHist.stream().map(obj -> DateUtil.yyyyMMdd.get().format(obj.getReportDate())).toArray(String[]::new);
			//logger.info(Arrays.toString(dates));
			//String[] dates2 = financeRecords.stream().map(obj -> DateUtil.yyyyMMdd.get().format(obj.getReportdate())).toArray(String[]::new);
			//logger.info(Arrays.toString(dates2));
			
			// 测试
			if (symbol.equals("sz000625")) {
				JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
				logger.info(JSON.toJSONString(stkQuarterViewWind, SerializerFeature.WriteDateUseDateFormat));
			}

			if (stkQuarterViewWind == null || stkQuarterViewWind.size() == 0) {
				logger.warn(symbol + " 没有有效的财务数据");
				return null;
			}

			// TODO:市盈率数据缺失问题可以忽略（只计算市净率）
			// 计算年化每股收益yearbasiceps
			int usefulFinRecordsNum = 0;
			//财报历史
			//XueqiuStockFinance[] finQRecords = financeRecords.toArray(new XueqiuStockFinance[0]);
			//股本变更历史
			//更新: 20171108 剔除股本变更中的财报日（@NOTE.这是错误的，财报日可能存在股本变更）
			/*
			Iterator<XueqiuStockShareschg> iter = sharesChgHist.iterator();
			while (iter.hasNext()) {
				XueqiuStockShareschg record = iter.next();
				String mmdd = DateUtil.MMdd.get().format(record.getBegindate());
				if ("0331".equals(mmdd) || "0630".equals(mmdd) || "0930".equals(mmdd) || "1231".equals(mmdd)) {
					iter.remove();
				}
			}
			//不包含财报日的股本变更历史
			XueqiuStockShareschg[] shareschgHist = sharesChgHist.toArray(new XueqiuStockShareschg[0]);
			*/

			//股本变更历史记录中可能包含财报日股本变更
			XueqiuStockShareschg[] shareschgHist = sharesChgHist.toArray(new XueqiuStockShareschg[0]);
			
			//问题：股本变更日07-05，半年报披露日08-10，此次股本变更是否影响半年报的财务指标？
			//股本变更日(begindate)应该与财报日（不是披露日）相比较，只影响财报期内的计算
			//案例：600104 2017-01-19存在股本变更，而2016年年报披露日期2017-04-06，2017-01-19的股本变更是否影响2016年报的计算
			//不属于财报期内的股本变更，不影响
			//阅读600104 2016年年报
			/*
			 * 2016年报 第六节
			 普通股股份变动情况表
				报告期内,公司普通股股份总数及股本结构未发生变化。2017 年 1 月 19 日,公司完成非公 开发行项目新增股份的登记,公司增加 657,894,736 股限售流通股,普通股股份总数变为 11,683,461,365 股。
				
				P110
				57、 股本
				
				P112
				注 2:资产负债表日后决议的利润分配情况
				根据本公司第六届董事会第十四次会议的提议,2016 年度按本公司当年度税后利润的 10%及 10%分别提取法定盈余公积金和任意盈余公积金后,以本公司 2016 年非公开发行之后的总股本 11,683,461,365.00 股为基准,每 10 股派送现金红利人民币 16.50 元(含税)。上述股利分配方案 尚有待股东大会批准。
			 */
			// @20180128 时间序列覆盖所有财报日（排序使用财报日）和股本变更日的财务指标历史
			//TODO: 股本变更日与财报日重叠的情况，合并为财报日，案例: sz300255
			StockQuarterViewWind finChgHistWind = new StockQuarterViewWind(symbol);
			for (int iFinQ = 0, iShareschg = 0; iFinQ < stkQuarterViewWind.size() && iShareschg < shareschgHist.length; ) {

				//XueqiuStockFinance fin = finQRecords[i];
				StockQuarterView finQ = stkQuarterViewWind.get(iFinQ);

				//财报日
				Date reportDate = finQ.getReportDate();
				//财报披露日
				//Date piluDate = finQ.getPiluDate();
				
				//test
				if (reportDate.equals(DateUtil.yyyyMMdd10.get().parse("2016-06-30"))) {
					logger.debug("test");
				}
				
				// TODO: assert默认不启用，不要使用assert
				//assert (quarterViewHist.get(i).getReportDate() == reportdate);

				// 雪球该指标数据NA较多不宜使用
				// BigDecimal basiceps = fin.getBasiceps();

				// 净利润
				Double netprofit1Y = stkQuarterViewWind.get(iFinQ).getNetprofit1Y();
				Double netprofit4Q = stkQuarterViewWind.get(iFinQ).getNetprofit4Q();
				Double netprofitExp = stkQuarterViewWind.get(iFinQ).getNetprofitExp();// 与上年同期相比的年净利润估值

				// EBIT
				Double EBIT1Y = stkQuarterViewWind.get(iFinQ).getEBIT1Y();
				Double EBIT4Q = stkQuarterViewWind.get(iFinQ).getEBIT4Q();
				
				//Date shareschgPublishdate = shareschgHist[iShareschg].getPublishdate();
				Date shareschgBegindate = shareschgHist[iShareschg].getBegindate();
				double totguben = 0;
				// 时间序列应覆盖所有财报日和股本变更日
				// 股本变更影响每股净资产naps，每股收益eps等的计算
				if (reportDate.equals(DateUtil.yyyyMMdd10.get().parse("2011-04-20"))) {
					logger.debug("测试sh600011");
				}
				
				//更新: 20171108
				//财报日与股本变更日重叠
				/*if (reportDate.compareTo(shareschgBegindate) == 0) {
					totguben = shareschgHist[iShareschg].getTotalshare() * 1e4;

					// 每股净资产
					double naps = finQ.getTotalShareEquity() / totguben;
					// 每股收益
					// TODO: 净利润为负值的情况
					double eps1Y = netprofit1Y / totguben;
					double eps4Q = netprofit4Q / totguben;
					double epsExp = netprofitExp / totguben;
					
					finQ.setNaps(naps);
					finQ.setEps1Y(eps1Y);
					finQ.setEps4Q(eps4Q);
					finQ.setEpsExp(epsExp);

					finChgHistWind.add(finQ);
					
					iShareschg++;
				}*/
				//if (reportDate.compareTo(shareschgdate) > 0) {
				//else 
				if (reportDate.compareTo(shareschgBegindate) > 0) {
					totguben = shareschgHist[iShareschg].getTotalshare() * 1e4;

					// 每股净资产
					double naps = finQ.getTotalShareEquity() / totguben;
					// 每股收益
					// TODO: 净利润为负值的情况
					double eps1Y = netprofit1Y / totguben;
					double eps4Q = netprofit4Q / totguben;
					double epsExp = netprofitExp / totguben;
					// fin.setEps4q(netprofit4q/totguben);

					// finchgdate.add(reportdate);
					// napschg.add(naps);
					// epschg.add(eps4q);

					/*
					StockQuarterView finchg = new StockQuarterView();
					finchg.setReportDate(reportDate);
					finchg.setPiluDate(piluDate);
					finchg.setNaps(naps);
					finchg.setEps1Y(eps1Y);
					finchg.setEps4Q(eps4Q);
					finchg.setEpsExp(epsExp);

					finChgHistWind.add(finchg);
					*/
					finQ.setNaps(naps);
					finQ.setEps1Y(eps1Y);
					finQ.setEps4Q(eps4Q);
					finQ.setEpsExp(epsExp);
					
					finQ.setEBIT1Y(EBIT1Y);
					finQ.setEBIT4Q(EBIT4Q);
					finQ.setTotalGuben(totguben);

					finChgHistWind.add(finQ);
					
					iFinQ++;

				} //else if (reportDate.compareTo(shareschgdate) == 0) { 
				else if (reportDate.compareTo(shareschgBegindate) == 0) { 
					totguben = shareschgHist[iShareschg].getTotalshare() * 1e4;

					// 每股净资产
					double naps = finQ.getTotalShareEquity() / totguben;
					// 每股收益
					// TODO: 净利润为负值的情况
					double eps1Y = netprofit1Y / totguben;
					double eps4Q = netprofit4Q / totguben;
					double epsExp = netprofitExp / totguben;
					// fin.setEps4q(netprofit4q/totguben);

					// finchgdate.add(reportdate);
					// napschg.add(naps);
					// epschg.add(eps4q);
					//
					
					/*
					StockQuarterView finchg = new StockQuarterView();
					finchg.setReportDate(reportDate);
					finchg.setPiluDate(piluDate);
					finchg.setNaps(naps);
					finchg.setEps1Y(eps1Y);
					finchg.setEps4Q(eps4Q);
					finchg.setEpsExp(epsExp);

					finChgHistWind.add(finchg);
					*/
					finQ.setNaps(naps);
					finQ.setEps1Y(eps1Y);
					finQ.setEps4Q(eps4Q);
					finQ.setEpsExp(epsExp);
					finQ.setType('X');
					
					finQ.setEBIT1Y(EBIT1Y);
					finQ.setEBIT4Q(EBIT4Q);
					finQ.setTotalGuben(totguben);

					finChgHistWind.add(finQ);

					iFinQ++;
					iShareschg++;

				} else {// TODO

					//此处不使用while也可以
					//while (reportDate.compareTo(shareschgHist[iShareschg].getBegindate()) < 0) {
						totguben = shareschgHist[iShareschg].getTotalshare() * 1e4;

						// 每股净资产
						double naps = finQ.getTotalShareEquity() / totguben;
						// 每股收益
						// TODO: 净利润为负值的情况
						double eps1Y = netprofit1Y / totguben;
						double eps4Q = netprofit4Q / totguben;
						double epsExp = netprofitExp / totguben;
						// fin.setEps4q(netprofit4q/totguben);

						// finchgdate.add(shareschgdate);
						// napschg.add(naps);
						// epschg.add(eps4q);

						StockQuarterView finchg = new StockQuarterView();
						finchg.setType('G');
						finchg.setReportDate(shareschgHist[iShareschg].getBegindate());
						finchg.setPiluDate(shareschgHist[iShareschg].getBegindate());//
						finchg.setNaps(naps);
						finchg.setEps1Y(eps1Y);
						finchg.setEps4Q(eps4Q);
						finchg.setEpsExp(epsExp);
						
						finchg.setNetprofit1Y(finQ.getNetprofit1Y());
						finchg.setNetprofit4Q(finQ.getNetprofit4Q());
						finchg.setNetprofitExp(finQ.getNetprofitExp());
						finchg.setWeightedroe1Y(finQ.getWeightedroe1Y());
						finchg.setWeightedroe4Q(finQ.getWeightedroe4Q());
						finchg.setWeightedroeExp(finQ.getWeightedroeExp());
						
						//
						finchg.setEBIT1Y(EBIT1Y);
						finchg.setEBIT4Q(EBIT4Q);
						finchg.setTotalGuben(totguben);

						finChgHistWind.add(finchg);

						iShareschg++;

				}

			}

			if (finChgHistWind.size() == 0) {
				logger.warn(symbol + " 没有有效的财务指标统计");
				return null;
			}
			
			//检验数据正确性
			/*
			for (int i = 0; i < finChgHistWind.size() - 1; i++) {
				if (finChgHistWind.get(i).getPiluDate().compareTo(finChgHistWind.get(i+1).getPiluDate()) < 0 ) {
					throw new RuntimeException(String.format("%s财务变更窗口数据错误: [%s - %s]", symbol,finChgHistWind.get(i).getPiluDate(),finChgHistWind.get(i+1).getPiluDate() ));
				}
			}
			*/
			
			for (int i = 0; i < finChgHistWind.size() - 1; i++) {
				if (finChgHistWind.get(i).getReportDate().compareTo(finChgHistWind.get(i+1).getReportDate()) < 0 ) {
					throw new RuntimeException(String.format("%s财务变更窗口数据错误: [%s - %s]", symbol,finChgHistWind.get(i).getReportDate(),finChgHistWind.get(i+1).getReportDate() ));
				}
			}
			
			
			//String[] dates3 = finQHist.stream().map(obj -> DateUtil.yyyyMMdd.get().format(obj.getReportDate())).toArray(String[]::new);
			//logger.info(Arrays.toString(dates3));

			//使用财务指标时，日期根据披露日
			// 总市值、流通市值、市盈率、市净率
			// PB/PE
			StockQuarterView[] finchghist = finChgHistWind.financeChgHist.toArray(new StockQuarterView[0]);
			List<TdxStockDay> dayHist = tdxStockDayMapper.selectBySymbol(symbol, true);

			/*
			List<XueqiuStockDayFq> dayFqHist = stockDayFMapper.selectBySymbol(symbol);
			if (dayHist.size() != dayFqHist.size() || !dayHist.get(0).getDate().equals(dayFqHist.get(0).getDate())
					|| !dayHist.get(dayHist.size() - 1).getDate().equals(dayFqHist.get(dayHist.size() - 1).getDate())) {
				logger.error(symbol + ": 复权数据与除权数据不一致********");
				return null;
			}
			*/

			TdxStockDay[] dayHistTs = dayHist.toArray(new TdxStockDay[0]);
			//XueqiuStockDayFq[] dayFqHistTs = dayFqHist.toArray(new XueqiuStockDayFq[0]);
			StockDayViewWind dayViewHist = new StockDayViewWind(symbol);
			//股本变更和财务报表历史记录
			dayViewHist.financeChgHistWind = finChgHistWind;
			
			int rowNo = 0;
			int iFinchg = 0, iShareschg = 0;
			double pb = 0d;
			double pe1Y = 0d, pe4Q = 0d, peExp = 0d;
			//
			Double MV2EBIT1Y = Double.NaN, MV2EBIT4Q = Double.NaN;
			double zongshizhi = 0d, liutongshizhi = 0d;
			for (int i = 0; i < dayHistTs.length; i++) {
				// for (TdxStockDay day : dayHist) {
				TdxStockDay day = dayHistTs[i];
				if (day.getDate().before(finchghist[finchghist.length - 1].getReportDate())) {
					break;
				}
				
				if (day.getDate().equals(DateUtil.yyyyMMdd10.get().parse("2017-01-03"))) {
					logger.debug("测试");
				}

				StockQuarterView qView = null;
				if (day.getDate().compareTo(finchghist[iFinchg].getReportDate()) >= 0) {
					qView = finchghist[iFinchg];
					pb = day.getClose() / finchghist[iFinchg].getNaps();
					pe1Y = day.getClose() / finchghist[iFinchg].getEps1Y();
					pe4Q = day.getClose() / finchghist[iFinchg].getEps4Q();
					peExp = day.getClose() / finchghist[iFinchg].getEpsExp();
					
					if (!DoubleCheck.checkHasNa(day.getClose(), finchghist[iFinchg].getTotalGuben(),
							finchghist[iFinchg].getEBIT1Y()))
						MV2EBIT1Y = day.getClose()*finchghist[iFinchg].getTotalGuben() / finchghist[iFinchg].getEBIT1Y();
					if (!DoubleCheck.checkHasNa(day.getClose(), finchghist[iFinchg].getTotalGuben(),
							finchghist[iFinchg].getEBIT4Q()))
						MV2EBIT4Q = day.getClose()*finchghist[iFinchg].getTotalGuben() / finchghist[iFinchg].getEBIT4Q();
					
				} else if (iFinchg < finchghist.length - 1) {
					logger.debug("财报日: " + finchghist[iFinchg].getReportDate());
					
					/*
					//年报和一季报同一天披露的很常见
					//TODO: 此处改为与交易日比较，跳过大于交易日的财务变更日
					int next = iFinchg;
					while (iFinchg+1 < finchghist.length && finchghist[next].getPiluDate().compareTo(finchghist[iFinchg+1].getPiluDate()) <= 0) {

						if (finchghist[next].getPiluDate().compareTo(finchghist[iFinchg+1].getPiluDate()) < 0)
							logger.error(String.format("**********************%s财报变更历史数据错误: [%s - %s]", symbol,finchghist[next].getPiluDate(),finchghist[iFinchg+1].getPiluDate()) );
						
						iFinchg++;
					}
					*/
					
					iFinchg++;

					//logger.info("财报日或股本变更日: " + DateUtil.yyyyMMdd.get().format(finchghist[iFinchg].getReportDate()));

					qView = finchghist[iFinchg];
					pb = day.getClose() / finchghist[iFinchg].getNaps();
					pe1Y = day.getClose() / finchghist[iFinchg].getEps1Y();
					pe4Q = day.getClose() / finchghist[iFinchg].getEps4Q();
					peExp = day.getClose() / finchghist[iFinchg].getEpsExp();
					
					if (!DoubleCheck.checkHasNa(day.getClose(), finchghist[iFinchg].getTotalGuben(),
							finchghist[iFinchg].getEBIT1Y()))
						MV2EBIT1Y = day.getClose()*finchghist[iFinchg].getTotalGuben() / finchghist[iFinchg].getEBIT1Y();
					if (!DoubleCheck.checkHasNa(day.getClose(), finchghist[iFinchg].getTotalGuben(),
							finchghist[iFinchg].getEBIT4Q()))
						MV2EBIT4Q = day.getClose()*finchghist[iFinchg].getTotalGuben() / finchghist[iFinchg].getEBIT4Q();
					
				} // else break;

				//总市值，流通市值
				//TODO: 统一使用finchghist此处不再使用shareschgHist
				/*
				if (day.getDate().compareTo(shareschgHist[iShareschg].getBegindate()) >= 0) {
					zongshizhi = day.getClose() * shareschgHist[iShareschg].getTotalshare();
					liutongshizhi = day.getClose() * shareschgHist[iShareschg].getCircskamt();
				} else if (iShareschg < shareschgHist.length - 1) {
					iShareschg++;

					zongshizhi = day.getClose() * shareschgHist[iShareschg].getTotalshare();
					liutongshizhi = day.getClose() * shareschgHist[iShareschg].getCircskamt();
				} // else break;

*/
				// pe负值标准化
				// if (pe < 0)
				// pe = normNegativePE(pe);

				StockDayView stats = new StockDayView();
				stats.setSymbol(symbol);
				stats.setDate(day.getDate());
				stats.setClose(dayHistTs[i].getClose());
				//stats.setFqClose(dayFqHistTs[i].getClose());
				stats.setPb(pb);
				stats.setPe1Y(pe1Y);
				stats.setPe4Q(pe4Q);
				stats.setPeExp(peExp);
				//stats.setZongshizhi(zongshizhi);
				//stats.setLiutongshizhi(liutongshizhi);
				
				stats.setMV2EBIT1Y(MV2EBIT1Y);
				stats.setMV2EBIT4Q(MV2EBIT4Q);

				stats.setQuarterView(qView);

				dayViewHist.add(stats);
				rowNo++;

			}

			return dayViewHist;

		} catch (Exception e) {
			logger.error(symbol + "出错", e);
			throw new RuntimeException(symbol + "出错", e);
		} finally {
			sqlSession.close();
			batchSqlSession.close();
		}

	}

	public static StockDayViewWind stats_index(String symbol) {
		StockDayViewWind dayViewHist = new StockDayViewWind(symbol);

		SqlSession sqlSession = null;
		try {

			sqlSession = sqlSessionFactory.openSession(true);

			TdxStockDayMapper stockDayMapper = sqlSession.getMapper(TdxStockDayMapper.class);

			List<TdxStockDay> dayHist = stockDayMapper.selectBySymbol(symbol, true);

			for (TdxStockDay day : dayHist) {
				StockDayView dv = new StockDayView();
				dv.setSymbol(symbol);
				dv.setDate(day.getDate());
				dv.setClose(day.getClose());

				dayViewHist.add(dv);
			}

			return dayViewHist;

		} catch (Exception e) {
			logger.error(symbol + "出错", e);
			throw new RuntimeException(symbol + "出错", e);
		} finally {
			sqlSession.close();
		}

	}

	private static double normNegativePE(double pe) {
		if (pe >= 0)
			return pe;

		return 1 / pe;
	}

	public static void copyFile(String oldPath, String newPath) {
		try {
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(oldPath);
			if (oldfile.exists()) {
				InputStream inStream = new FileInputStream(oldPath);
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1444];
				int length;
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
				fs.flush();
				fs.close();
			}
		} catch (Exception e) {
			System.out.println("error  ");
			e.printStackTrace();
		}
	}

	public static Date latestTxnDate() {

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());

		int day = calendar.get(Calendar.DAY_OF_WEEK);
		if (day == 1) {
			calendar.add(Calendar.DAY_OF_MONTH, -2);
		} else if (day == 7) {
			calendar.add(Calendar.DAY_OF_MONTH, -1);
		}

		return calendar.getTime();

	}

	public static void main(String[] args) {
		//严格检查数据正确性
		String symbol = "sh600887";// sz000625,sh600104
		StockDayViewWind dayViewWind = stats(symbol);

		String[] date = dayViewWind.dayViewHist.stream()
				.map(obj -> "'" + DateUtil.yyyyMMdd10.get().format(obj.getDate()) + "'").toArray(String[]::new);
		double[] pe1Y = dayViewWind.dayViewHist.stream().mapToDouble(obj -> obj.getPe1Y()).toArray();
		double[] pe4Q = dayViewWind.dayViewHist.stream().mapToDouble(obj -> obj.getPe4Q()).toArray();

		System.out.println("date <- as.Date( c" + Arrays.toString(date).replace('[', '(').replace(']', ')') + ")");
		System.out.println("pe1Y <- c" + Arrays.toString(pe1Y).replace('[', '(').replace(']', ')'));
		System.out.println("pe4Q <- c" + Arrays.toString(pe4Q).replace('[', '(').replace(']', ')'));

		// plot(date[1:1500],pe4Q[1:1500],type='l')
		// pe评估：至少3年数据
		// pe连续性、有效性 （正相关性，负相关性）
		// 评估3种pe计算方式的有效程度
		// cor(df$close[1:1500],pe4Q[1:1500])
		// cor(logR,pe)

	}

}
