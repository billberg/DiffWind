package com.diffwind.stats;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.diffwind.dao.mapper.QueryMapper;
import com.diffwind.dao.mapper.XueqiuStockFinanceMapper;
import com.diffwind.dao.model.XueqiuStockFinance;
import com.diffwind.stats.filters.MingxingCasesFilter;
import com.diffwind.util.DateUtil;
import com.diffwind.util.DoubleCheck;

/**
 * 季度统计，内存统计结果不持久化
 * 
 * V2: 补充财报披露日
 * @20180128 TODO: 不再使用披露日。该统计虽使用但无影响
 * 
 * @author billberg
 *
 */
public class MemStatsStockQuarter {

	private static Logger logger = Logger.getLogger(MemStatsStockQuarter.class);

	private static SqlSessionFactory sqlSessionFactory = null;
	private static SqlSessionFactory batchSqlSessionFactory = null;

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
	 * 季度统计
	 * 统计指标：净利润，净资产收益率
	 * TODO: 指标计算方式待优化，ROE的滚动计算方式不准确
	 * TODO 负值处理 同期比值过于悬殊的问题 财报披露的时间滞后问题
	 * 财报数据不足截断窗口问题: NAN
	 * 
	 * ROE: 1Y/3Y/5Y ROA: 1Y/3Y/5Y netprofit: 1y/3y/5y
	 * 
	 * @param symbol
	 */
	public static StockQuarterViewWind stats(String symbol) {
		SqlSession sqlSession = null;
		SqlSession batchSqlSession = null;

		try {

			StockQuarterViewWind quarterHist = new StockQuarterViewWind(symbol);

			sqlSession = sqlSessionFactory.openSession(true);
			batchSqlSession = batchSqlSessionFactory.openSession(ExecutorType.BATCH, false);

			XueqiuStockFinanceMapper xueqiuFinanceMapper = sqlSession.getMapper(XueqiuStockFinanceMapper.class);
			QueryMapper queryMapper = sqlSession.getMapper(QueryMapper.class);

			List<XueqiuStockFinance> financeRecords = xueqiuFinanceMapper.selectBySymbol(symbol);
			
			//TODO: allStockInfo放到公共类
			String hyname = MingxingCasesFilter.allStockInfo.get(symbol).get("hyname").toString();
			
			//统计中使用到的利润表科目：粗估EBIT
			List<Map> isRecords = null;
			if ("金融行业".equals(hyname)) {
				//金融行业利润表科目
				isRecords = queryMapper.selectIncomeStatementBySymbol_JinRong(symbol);
			} else {
				isRecords = queryMapper.selectIncomeStatementBySymbol(symbol);
			}
			

			if (financeRecords == null || financeRecords.size() == 0) {
				logger.warn(symbol + " 没有有效的财务数据");
				return quarterHist;
			}
			
			//日期对齐检查
			if (!DateUtil.yyyyMMdd.get().format(financeRecords.get(0).getReportdate()).equals((String)isRecords.get(0).get("报表期截止日")) ) {
				logger.error(String.format(symbol + " 主要财务指标表与利润表日期不一致: %s, %s",DateUtil.yyyyMMdd.get().format(financeRecords.get(0).getReportdate()), (String)isRecords.get(0).get("报表期截止日") ));
				
				return quarterHist;
			}
			
			//补充缺失的财报披露日（不严格按照披露最后一日）
			for (int i = 0; i < financeRecords.size(); i++) {
				XueqiuStockFinance f = financeRecords.get(i);
				if (f.getPiluDate() == null) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(f.getReportdate());
					int month = calendar.get(Calendar.MONTH) + 1;
					
					if (month == 3 || month == 9) {//
						calendar.add(Calendar.MONTH, 1);
						f.setPiluDate(calendar.getTime());//04-30/10-30
					} else if (month == 6) {
						calendar.add(Calendar.MONTH, 2);
						f.setPiluDate(calendar.getTime());//08-30
					} else if (month == 12) {
						calendar.add(Calendar.MONTH, 4);
						f.setPiluDate(calendar.getTime());//04-30
					}
					
					//确保披露日有序排序
					if (i-1 >= 0) {
						Date nextPiluDate = financeRecords.get(i-1).getPiluDate();
						if (f.getPiluDate().after(nextPiluDate) ) {
							calendar.setTime(nextPiluDate);
							calendar.add(Calendar.DAY_OF_MONTH, -1);
							f.setPiluDate(calendar.getTime());
						}
					}
				}
			}

			// TODO:
			// 计算年化每股收益yearbasiceps
			int usefulFinRecordsNum = 0;
			XueqiuStockFinance[] finQRecords = financeRecords.toArray(new XueqiuStockFinance[0]);

			for (int i = 0; i < finQRecords.length; i++) {

				XueqiuStockFinance finQ = finQRecords[i];

				// BigDecimal netprofit = fin.getNetprofit();
				if (finQ.getNetprofit() == null || finQ.getTotsharequi() == null) {
					usefulFinRecordsNum = i;
					break;
				}

				// 雪球该指标数据na较多不宜使用
				// BigDecimal basiceps = fin.getBasiceps();
				/**** 盈利指标 ****/
				// 上年净利润
				Double netprofit1Y = Double.NaN;
				// 上4个季度净利润
				Double netprofit4Q = Double.NaN;
				// 预期年净利润
				Double netprofitExp = Double.NaN;// 与上年同期相比的年净利润估值

				// 上年ROE
				Double weightedroe1Y = Double.NaN;
				// 上4个季度ROE
				Double weightedroe4Q = Double.NaN;
				// 预期年ROE
				Double weightedroeExp = Double.NaN;// 与上年同期相比的年净资产收益率估值
				
				//@20180302
				// 上年EBIT
				Double EBIT1Y = Double.NaN;
				// 上4个季度EBIT
				Double EBIT4Q = Double.NaN;

				// 季报月份
				Date reportDate = finQ.getReportdate();
				Date piluDate = finQ.getPiluDate();
				Double totalAssets = finQ.getTotalassets();
				Double netAssetsGrowthRate = finQ.getNetassgrowrate();
				Double totalAssetsGrowthRate = finQ.getTotassgrowrate();

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(finQ.getReportdate());
				int month = calendar.get(Calendar.MONTH) + 1;

				// XueqiuStockFinance finQ = finQRecords[i];// 当前季度
				XueqiuStockFinance finLastY = null, finLastSameQ = null;
				//if (i + month / 3 < finQRecords.length - 1) {
				if (i + month / 3 < finQRecords.length) {
					finLastY = finQRecords[i + month / 3];// 上年年终
				}
				//if (i + 4 < finQRecords.length - 1) {
				if (i + 4 < finQRecords.length) {
					finLastSameQ = finQRecords[i + 4];// 上年同期
				}
				
				//利润表记录
				Map isLastY = null, isLastSameQ = null;
				if (i + month / 3 < isRecords.size()) {
					isLastY = isRecords.get(i + month / 3);// 上年年终
				}
				if (i + 4 < isRecords.size()) {
					isLastSameQ = isRecords.get(i + 4);// 上年同期
				}

				if (month == 12) {
					netprofit1Y = netprofit4Q = netprofitExp = finQ.getNetprofit();
					weightedroe1Y = weightedroe4Q = weightedroeExp = finQ.getWeightedroe();
					
					//
					EBIT1Y = EBIT4Q = (Double)isRecords.get(i).get("EBIT");
				} else {
					if (finLastY != null) {
						netprofit1Y = finLastY.getNetprofit();
						weightedroe1Y = finLastY.getWeightedroe();
					}
 
					if (finLastY != null && finLastSameQ != null) {
						boolean hasNa = DoubleCheck.checkHasNa(finLastY.getNetprofit(), finQ.getNetprofit(),
								finLastSameQ.getNetprofit());
						if (!hasNa) {
							netprofit4Q = finLastY.getNetprofit() + finQ.getNetprofit() - finLastSameQ.getNetprofit();
						}
						
						hasNa = DoubleCheck.checkHasNa(finLastY.getWeightedroe(), finQ.getWeightedroe(),
								finLastSameQ.getWeightedroe());
						if (!hasNa) {
							//TODO: ROE的滚动计算方式不合理
							weightedroe4Q = finLastY.getWeightedroe() + finQ.getWeightedroe() - finLastSameQ.getWeightedroe();
						}
					}
					
					
					//
					if (isLastY != null) {
						EBIT1Y = (Double)isLastY.get("EBIT");
					}
 
					if (isLastY != null && isLastSameQ != null) {
						boolean hasNa = DoubleCheck.checkHasNa((Double)isLastY.get("EBIT"), (Double)isRecords.get(i).get("EBIT"),
								(Double)isLastSameQ.get("EBIT"));
						if (!hasNa) {
							EBIT4Q = (Double)isLastY.get("EBIT") + (Double)isRecords.get(i).get("EBIT") - (Double)isLastSameQ.get("EBIT");
						}
					}
				}

				// 预期指标的计算
				if (month == 3) {
					netprofitExp = netprofit4Q;
					weightedroeExp = weightedroe4Q;
				} else if (month == 6 || month == 9) {

					// 预期保守一些
					if (finLastY != null && finLastSameQ != null) {
						boolean hasNa = DoubleCheck.checkHasNa(finLastY.getNetprofit(), finQ.getNetprofit(),
								finLastSameQ.getNetprofit());
						if (!hasNa) {
							boolean isPositive = DoubleCheck.checkPositive(finLastY.getNetprofit(), finQ.getNetprofit(),
									finLastSameQ.getNetprofit());
							if (isPositive) {
	
								if (finQ.getNetprofit() / finLastSameQ.getNetprofit() >= 1) { // 盈利预期超过上年
									netprofitExp = finLastY.getNetprofit() * finQ.getNetprofit()
											/ finLastSameQ.getNetprofit();
								} else { // 盈利预期低于上年
	
									// 2Q/3Q报表有较强预期价值
									if ((month == 6 && finLastSameQ.getNetprofit() / finLastY.getNetprofit() > 0.35)
											|| (month == 9
													&& finLastSameQ.getNetprofit() / finLastY.getNetprofit() > 0.5)) {
										netprofitExp = finLastY.getNetprofit() * finQ.getNetprofit()
												/ finLastSameQ.getNetprofit();
									} else {
										netprofitExp = finQ.getNetprofit() * 12 / month;
									}
								}
							}
						}

						hasNa = DoubleCheck.checkHasNa(finLastY.getWeightedroe(), finQ.getWeightedroe(),
								finLastSameQ.getWeightedroe());
						if (!hasNa) {
							boolean isPositive = DoubleCheck.checkPositive(finLastY.getWeightedroe(), finQ.getWeightedroe(),
									finLastSameQ.getWeightedroe());
							if (isPositive) {
	
								if (finQ.getWeightedroe() / finLastSameQ.getWeightedroe() >= 1) { // 盈利预期超过上年
									weightedroeExp = finLastY.getWeightedroe() * finQ.getWeightedroe()
											/ finLastSameQ.getWeightedroe();
								} else { // 盈利预期低于上年
	
									// 2Q/3Q报表有较强预期价值
									if (((month == 6 && finLastSameQ.getWeightedroe() / finLastY.getWeightedroe() > 0.35)
											|| (month == 9
													&& finLastSameQ.getWeightedroe() / finLastY.getWeightedroe() > 0.5)))
										weightedroeExp = finLastY.getWeightedroe() * finQ.getWeightedroe()
												/ finLastSameQ.getWeightedroe();
									else {
										weightedroeExp = finQ.getWeightedroe() * 12 / month;
									}
								}
							}
						}

						// 预期相比滚动盈利指标取最小值
						netprofitExp = Math.min(netprofit4Q, netprofitExp);
						weightedroeExp = Math.min(weightedroe4Q, weightedroeExp);
					
					}

				}

				if (Double.isNaN(netprofit4Q)) {
					logger.debug("无效的财务指标: " + DateUtil.yyyyMMdd10.get().format(reportDate));
				}
				
				StockQuarterView statsQ = new StockQuarterView();
				statsQ.setSymbol(symbol);
				statsQ.setReportDate(reportDate);
				statsQ.setPiluDate(piluDate);//
				statsQ.setNetprofit1Y(DoubleCheck.ifNull2NaN(netprofit1Y));
				statsQ.setNetprofit4Q(DoubleCheck.ifNull2NaN(netprofit4Q));
				statsQ.setNetprofitExp(DoubleCheck.ifNull2NaN(netprofitExp));
				statsQ.setWeightedroe1Y(DoubleCheck.ifNull2NaN(weightedroe1Y));
				statsQ.setWeightedroe4Q(DoubleCheck.ifNull2NaN(weightedroe4Q));
				statsQ.setWeightedroeExp(DoubleCheck.ifNull2NaN(weightedroeExp));
				statsQ.setTotalAssets(DoubleCheck.ifNull2NaN(totalAssets));
				statsQ.setNetAssetsGrowthRate(DoubleCheck.ifNull2NaN(netAssetsGrowthRate));
				statsQ.setTotalAssetsGrowthRate(DoubleCheck.ifNull2NaN(totalAssetsGrowthRate));
				//所有者权益
				statsQ.setTotalShareEquity(DoubleCheck.ifNull2NaN(finQ.getTotsharequi()));
				
				//季报净利润
				statsQ.setNetprofit(finQ.getNetprofit());
				//主营业务收入
				statsQ.setMainBizIncome(finQ.getMainbusiincome());
				//主营业务收入同比增长率
				statsQ.setMainBizIncomeGrowthRate(finQ.getMainbusincgrowrate());
				
				//EBIT
				statsQ.setEBIT((Double)isRecords.get(i).get("EBIT"));
				statsQ.setEBIT1Y(EBIT1Y);
				statsQ.setEBIT4Q(EBIT4Q);
				
				//缴税
				statsQ.setJingyingShui((Double)isRecords.get(i).get("税金及附加"));
				statsQ.setSuodeShui((Double)isRecords.get(i).get("所得税费用"));

				quarterHist.add(statsQ);

			}

			return quarterHist;
		} catch (Exception e) {
			logger.error(symbol + "出错", e);
			throw new RuntimeException(symbol + "出错", e);
		} finally {
			sqlSession.close();
			batchSqlSession.close();
		}
	}

}
