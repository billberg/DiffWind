package com.diffwind.stats.filters;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.diffwind.dao.mapper.QueryMapper;
import com.diffwind.dao.mapper.SinaZjhhangyeStockMapper;
import com.diffwind.dao.model.SinaZjhhangyeStock;
import com.diffwind.expr.ExpressRunnerFactory;
import com.diffwind.stats.MemStatsStockDay;
import com.diffwind.stats.MemStatsStockQuarter;
import com.diffwind.stats.StockDayView;
import com.diffwind.stats.StockDayViewWind;
import com.diffwind.stats.StockQuarterView;
import com.diffwind.stats.StockQuarterViewWind;
import com.diffwind.util.DateUtil;
import com.diffwind.util.DoubleCheck;
import com.diffwind.util.ExcelUtil;
import com.diffwind.util.Functions;
import com.diffwind.util.RiskProb;
import com.google.common.primitives.Doubles;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;

import sort.CollectionTest.Man;

/**
 * 根据指标筛选
 * 
 * @author billberg
 *
 */
public class MingxingCasesFilter {

	private static Logger logger = Logger.getLogger(MingxingCasesFilter.class);

	private static SqlSessionFactory sqlSessionFactory = null;
	// private static SqlSessionFactory batchSqlSessionFactory = null;

	//private static List<SinaZjhhangyeStock> allStocks = null;
	//private static Map<String, SinaZjhhangyeStock> stockHangye = new HashMap<String, SinaZjhhangyeStock>();
	
	public static Map<String, Map<String, Object>> allStockInfo = new TreeMap<String, Map<String, Object>>();

	static {
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),
					"simpleds");

			SqlSession sqlSession = sqlSessionFactory.openSession(true);
			
			/*
			SinaZjhhangyeStockMapper stockHangyeMapper = sqlSession.getMapper(SinaZjhhangyeStockMapper.class);
			allStocks = stockHangyeMapper.selectAllStockHangye();
			for (SinaZjhhangyeStock stkHangye : allStocks) {
				stockHangye.put(stkHangye.getSymbol(), stkHangye);
			}
			*/
			
			String sql = "with q as (select symbol, name, string_agg(hyname,',' order by hycode desc) \"hyname\" "
						+ " from sina_zjhhangye_stock group by symbol,name)"
						+ " select q.symbol,q.name, q.hyname, c.shangshi_date, c.chengli_date, c.zuzhixingshi, c.zhuce_addr, c.jingyingfanwei"
						+ " from q, sina_stock_corp_info c"
						+ " where q.symbol = c.symbol"
						+ " order by q.symbol";
			
			
			PreparedStatement preparedStatement = sqlSession.getConnection().
					prepareStatement(sql);
			
			ResultSet resultSet = preparedStatement.executeQuery();
			
			
			ResultSetMetaData rsmd = resultSet.getMetaData();
			int count = rsmd.getColumnCount();
			String key=null;
			while(resultSet.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				for(int i = 1; i <= count; i++){
					
					key = rsmd.getColumnName(i).toLowerCase();
					map.put(key, resultSet.getObject(i));
					
				}
				allStockInfo.put(map.get("symbol").toString(), map);
			}

			sqlSession.close();

		} catch (Exception e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 10, TimeUnit.SECONDS, workQueue);

	static ExpressRunner runner = ExpressRunnerFactory.getInstance();

	

	/**
	 * 在最近一个窗口内搜索
	 * 排除窗口内存在亏损记录的公司
	 * TODO: 增加公司简介
	 * 
	 * @param w 窗口大小
	 */
	@Deprecated
	public static void filter(boolean isChart) {

		// List<String> symbols = new Vector<String>();
		List<Object[]> tableData = new Vector<Object[]>();

		final CountDownLatch countDownLatch = new CountDownLatch(allStockInfo.size());
		for (final String symbol : allStockInfo.keySet()) {

			executor.execute(new Runnable() {

				@Override
				public void run() {

					//String symbol = stk.getSymbol();
					try {

						StockDayViewWind stockDayViewWind = MemStatsStockDay.stats(symbol);
						if (stockDayViewWind == null || stockDayViewWind.size() < 240*5
								|| stockDayViewWind.financeChgHistWind == null
								|| stockDayViewWind.financeChgHistWind.size() < 20) {
							return;
						}
						/*
						if (stockDayViewWind == null 
								|| stockDayViewWind.quarterViewWind == null
								|| stockDayViewWind.quarterViewWind.size() < 20) {
							return;
						}*/
						
						StockQuarterViewWind stockQuarterViewWind = stockDayViewWind.financeChgHistWind;
						
						// 排除近5年存在亏损记录的公司，此处使用年报指标不使用4Q季度滚动指标，滚动指标容易出现负值
						List<StockQuarterView> quarterViewHist = stockQuarterViewWind.quarterFinanceHist;
						Double[] v_netprofit = new Double[5];
						Double[] v_roe = new Double[5];
						Double[] v_ganggan = new Double[5];
						for (int i = 0; i < 5 && i*4 < quarterViewHist.size(); i++) {
							Double netprofit1Y = quarterViewHist.get(i*4).getNetprofit1Y();
							Double roe1Y = quarterViewHist.get(i*4).getWeightedroe1Y();
							//资产没有使用年报，不严谨
							Double ganggan = quarterViewHist.get(i*4).getTotalAssets()/quarterViewHist.get(i*4).getTotalShareEquity();
							v_netprofit[i] = netprofit1Y;
							v_roe[i] = roe1Y;
							v_ganggan[i] = ganggan;
						}
						
						
						// 分类
						IExpressContext<String, Object> expressContext = new DefaultContext<String, Object>();
						expressContext.put("v_netprofit", v_netprofit);
						expressContext.put("v_roe", v_roe);

						Boolean isMatched = (Boolean) runner.execute(FilterRules.MINGXING_5Y, expressContext, null, false,
									false);

						if (isMatched) {
								//netprofit, roe, 杠杆率
							String name = allStockInfo.get(symbol).get("name").toString();
							String hyname = allStockInfo.get(symbol).get("hyname").toString();
							Object shangshi_date = allStockInfo.get(symbol).get("shangshi_date");
							Object chengli_date = allStockInfo.get(symbol).get("chengli_date");
							String zuzhixingshi = allStockInfo.get(symbol).get("zuzhixingshi").toString();
							String zhuce_addr = allStockInfo.get(symbol).get("zhuce_addr").toString();
							String jingyingfanwei = allStockInfo.get(symbol).get("jingyingfanwei").toString();
							
							tableData.add(new Object[] {stockDayViewWind.get(0).getDate(), symbol, name, hyname,
									//TODO公司简介
									shangshi_date, chengli_date,zuzhixingshi,zhuce_addr,jingyingfanwei,
									stockDayViewWind.get(0).getPe1Y(),stockDayViewWind.get(0).getPe4Q(),
										v_netprofit[0], v_roe[0], v_ganggan[0],
										v_netprofit[1], v_roe[1], v_ganggan[1],
										v_netprofit[2], v_roe[2], v_ganggan[2],
										v_netprofit[3], v_roe[3], v_ganggan[3],
										v_netprofit[4], v_roe[4], v_ganggan[4]

							});
						}


					} catch (Exception e) {
						logger.error("filter()出错: " + symbol, e);
					} finally {
						countDownLatch.countDown();

						logger.info(symbol + " 结束, 剩余: " + countDownLatch.getCount());
					}

				}
			});

			while (workQueue.size() > 90) {
				logger.info("waiting...");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// 等待所有任务执行结束
		try {

			countDownLatch.await();

			// 输出文件
			String outputFileName = "filters.output/MingxingCasesFilter-"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xls";

			tableData.add(0, new String[] {"日期","代码", "名称", "行业",
					"上市日期","成立日期","组织形式","注册地址","经营范围",
					"PE1Y","PE4Q",
					"2016\n净利润","2016\nROE","2016\n杠杆率",
					"2015\n净利润","2015\nROE","2015\n杠杆率",
					"2014\n净利润","2014\nROE","2014\n杠杆率",
					"2013\n净利润","2013\nROE","2013\n杠杆率",
					"2012\n净利润","2012\nROE","2012\n杠杆率"
			});

			// TODO: 市盈率，市净率的风险水位：a.根据日线样本排序计算下跌概率 b.根据值计算下跌空间，下跌/上涨空间比
			ExcelUtil.exportTables2Excel(new String[] { "案例" }, new List[] { tableData }, true, true,
					outputFileName);

		} catch (Exception e) {
			logger.error("筛选出错", e);
		} finally {
			//
		}
	}

	
	/**
	 * @20180214 更新: 改成2步筛选 1.所得税 2.盈利指标
	 * @201803 更新: 添加MV/EBIT，营收增长率，EBIT增长率
	 * @20180311 更新: 添加经营税（税金及附加）增长率，所得税（所得税费用）增长率
	 * TODO: 
	 * 添加指标：PEG
	 * 关键指标输出季度同比增长率
	 * 指标尽量用包装类型，不用原始类型，以处理数据缺失
	 * @param years
	 * @param isChart
	 */
	public static void filterByY(int years, boolean isChart) {

		// List<String> symbols = new Vector<String>();
		
		//String[] yearsName = new String[years];
		//List<String> yearsName = new ArrayList<String>();
		
		//@20180214 更新
		String MINGXING_EXPR = MessageFormat.format(FilterRules.YINGLI2_CLASS_$Y, years);
		
		List<Object[]> mingxingCases = new Vector<Object[]>();
		List<Object[]> highNetprofitCases = new Vector<Object[]>();
		List<Object[]> highRoeCases = new Vector<Object[]>();
		//高成长
		List<Object[]> highGrowthCases = new Vector<Object[]>();
		//良好
		List<Object[]> lianghaoCases = new Vector<Object[]>();
		//关注
		List<Object[]> guanzhuCases = new Vector<Object[]>();
		
		//根据利润表与税金筛选结果
		Map<String, List<Map>>  suodeshuiSatisfiedStocks = new HashMap<String, List<Map>>();
		SqlSession sqlSession = null;
		try {
			
			sqlSession = sqlSessionFactory.openSession(true);
			
			QueryMapper queryMapper = sqlSession.getMapper(QueryMapper.class);
			
			
			/*
			List<Map> records = queryMapper.selectBySuodeshui();
			String symbol = "#";
			//多年记录
			List<Map> stockRecords = new ArrayList<Map>();
			for (Map record : records) {
				//for test
				//if ("sh603288".equals((String)record.get("symbol")))
				
				if (!symbol.equals((String)record.get("symbol")) ) {
					symbol = (String)record.get("symbol");
					stockRecords = new ArrayList<Map>();
					suodeshuiSatisfiedStocks.put(symbol, stockRecords);
				} 
				
				stockRecords.add(record);
			}
			*/
			
			//@20180305
			//TODO: 改为并发查询
			//性能优化，改为分行业批量查询
			/*
			final CountDownLatch countDownLatch = new CountDownLatch(allStockInfo.size());
			for (final String symbol : allStockInfo.keySet()) {
				
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							String hyname = allStockInfo.get(symbol).get("hyname").toString();
							List<Map> stockRecords = null;
							if ("金融行业".equals(hyname)) {
								stockRecords = queryMapper.selectByShui_JinRong(symbol);
							} else {
								stockRecords = queryMapper.selectByShui(symbol);
							}
							
							if (stockRecords.size() == 3) {
								suodeshuiSatisfiedStocks.put(symbol, stockRecords);
							}
						
						} finally {
							countDownLatch.countDown();

							logger.info("初筛进度, 剩余: " + countDownLatch.getCount());
						}
					}
				
				});
				
				while (workQueue.size() > 90) {
					logger.info("waiting...");

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			
			}
			
			
			countDownLatch.await();
			*/
			
			//@20180306
			Map<String, List> hyStocks = new HashMap<String, List>();
			for (final String symbol : allStockInfo.keySet()) {
				String hyname = allStockInfo.get(symbol).get("hyname").toString();
				if ("金融行业".equals(hyname)) {
					if (!hyStocks.containsKey("金融行业")) {
						List<String> symbols = new ArrayList<String>();
						symbols.add(symbol);
						hyStocks.put("金融行业", symbols);
					} else {
						List<String> symbols = hyStocks.get("金融行业");
						symbols.add(symbol);
					}
				} else {
					if (!hyStocks.containsKey("其它行业")) {
						List<String> symbols = new ArrayList<String>();
						symbols.add(symbol);
						hyStocks.put("其它行业", symbols);
					} else {
						List<String> symbols = hyStocks.get("其它行业");
						symbols.add(symbol);
					}
				}
			}
							
			
			for (String hyname : hyStocks.keySet()) {
				List<Map> records = null;
				if ("金融行业".equals(hyname)) {
					records = queryMapper.selectBySuodeshui_JinRong(hyStocks.get(hyname));
				} else {
					records = queryMapper.selectBySuodeshui(hyStocks.get(hyname));
				}
				
				String symbol = "#";
				//多年记录
				List<Map> stockRecords = new ArrayList<Map>();
				for (Map record : records) {
					//for test
					//if ("sh603288".equals((String)record.get("symbol")))
					if (!symbol.equals((String)record.get("symbol")) ) {
						symbol = (String)record.get("symbol");
						stockRecords = new ArrayList<Map>();
						suodeshuiSatisfiedStocks.put(symbol, stockRecords);
					} 
					
					stockRecords.add(record);
				}
			
			}

		} catch (Exception e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		} finally {
			sqlSession.close();
		}
		
		
		//test
		//allStockInfo.clear();
		//allStockInfo.put("sz000001", null);
		final CountDownLatch countDownLatch = new CountDownLatch(suodeshuiSatisfiedStocks.size());
		//for (final String symbol : allStockInfo.keySet()) {
		for (final String symbol : suodeshuiSatisfiedStocks.keySet()) {

			executor.execute(new Runnable() {

				@Override
				public void run() {

					try {

						StockDayViewWind stockDayViewWind = MemStatsStockDay.stats(symbol);
						
						if (stockDayViewWind == null 
								|| stockDayViewWind.financeChgHistWind == null
								|| stockDayViewWind.financeChgHistWind.yearFinanceHist.size() < years) {
							return;
						}
						
						// 此处使用年报指标不使用4Q季度滚动指标，滚动指标容易出现负值
						List<StockQuarterView> yearFinanceHist = stockDayViewWind.financeChgHistWind.yearFinanceHist;
						
						//使用[0Y,-1Y,-2Y...]
						/*
						if ("sz000001".equals(symbol)) {
							for (int i = 0; i < years; i++) {
								//TODO: 如果存在停牌1年以上的情况，年度可能对不上
								//yearsName[i] = "" + yearFinanceHist.get(i).getReportYear();
								yearsName.add("" + yearFinanceHist.get(i).getReportYear());
							}
						}*/
						
						//矩阵
						//int w = yearFinanceHist.size();
						int w = Math.min(years+5, yearFinanceHist.size());
						Double[] v_netprofit = new Double[w];
						Double[] v_roe = new Double[w];
						Double[] v_ganggan = new Double[w];
						//
						Double[] v_totalAssets = new Double[w];
						Double[] v_totalShareEquity = new Double[w];
						Double[] v_mainBizIncome = new Double[w];
						for (int i = 0; i < w; i++) {
							Double netprofit1Y = yearFinanceHist.get(i).getNetprofit1Y();
							Double roe1Y = yearFinanceHist.get(i).getWeightedroe1Y();
							Double ganggan = Double.NaN;
							if (!DoubleCheck.checkHasNa(yearFinanceHist.get(i).getTotalAssets(), yearFinanceHist.get(i).getTotalShareEquity())) {
								ganggan = yearFinanceHist.get(i).getTotalAssets()/yearFinanceHist.get(i).getTotalShareEquity();
							}
							
							v_netprofit[i] = netprofit1Y;
							v_roe[i] = roe1Y;
							v_ganggan[i] = ganggan;
							v_totalAssets[i] = yearFinanceHist.get(i).getTotalAssets();
							v_totalShareEquity[i] = yearFinanceHist.get(i).getTotalShareEquity();
							v_mainBizIncome[i] = yearFinanceHist.get(i).getMainBizIncome();
						}
						
						// 分类
						IExpressContext<String, Object> expressContext = new DefaultContext<String, Object>();
						expressContext.put("v_netprofit", v_netprofit);
						expressContext.put("v_roe", v_roe);
						expressContext.put("v_totalAssets", v_totalAssets);
						expressContext.put("v_totalNetAssets", v_totalShareEquity);
						
						//总资产增长率
						double[] v_totalAssetsGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_totalAssets), -1),100);
						//净资产增长率
						double[] v_totalShareEquityGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_totalShareEquity), -1),100);
						//净利润增长率
						double[] v_netprofitGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_netprofit), -1),100);
						//主营业务收入增长率，原始指标数据缺失较多
						//Double[] v_mainBizIncomeGrowthRate = yearFinanceHist.stream().map(obj -> obj.getMainBizIncomeGrowthRate()).toArray(Double[]::new);
						double[] v_mainBizIncomeGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_mainBizIncome), -1),100);
						
						expressContext.put("v_totalAssetsGrowthRate", ArrayUtils.toObject(v_totalAssetsGrowthRate));
						expressContext.put("v_totalNetAssetsGrowthRate", ArrayUtils.toObject(v_totalShareEquityGrowthRate));
						expressContext.put("v_mainBizIncomeGrowthRate", ArrayUtils.toObject(v_mainBizIncomeGrowthRate));
						
						
						//Boolean isMatched = (Boolean) runner.execute(MINGXING_EXPR, expressContext, null, false, false);
						//TODO: NPE
						Integer classify = (Integer) runner.execute(MINGXING_EXPR, expressContext, null, false,
								false);

						if (classify <= 6) {
							//netprofit, roe, 杠杆率
							String name = allStockInfo.get(symbol).get("name").toString();
							String hyname = allStockInfo.get(symbol).get("hyname").toString();
							Object shangshi_date = allStockInfo.get(symbol).get("shangshi_date");
							Object chengli_date = allStockInfo.get(symbol).get("chengli_date");
							String zuzhixingshi = allStockInfo.get(symbol).get("zuzhixingshi").toString();
							String zhuce_addr = allStockInfo.get(symbol).get("zhuce_addr").toString();
							String jingyingfanwei = allStockInfo.get(symbol).get("jingyingfanwei").toString();
							
							
							//计算风险水位
							//years=10Y窗口
							Date toDate = stockDayViewWind.get(0).getDate();
							Calendar cal = Calendar.getInstance();
							cal.setTime(toDate);
							cal.add(Calendar.YEAR, -years);
							Date fromDate = cal.getTime();
						
							List<StockDayView> histWind = stockDayViewWind.rangeOf(fromDate, toDate);
							double[] v_pe1Y = histWind.stream().mapToDouble(obj -> obj.getPe1Y()).toArray();
							double[] v_pe4Q = histWind.stream().mapToDouble(obj -> obj.getPe4Q()).toArray();
							double[] v_pb = histWind.stream().mapToDouble(obj -> obj.getPb()).toArray();
							
							double[] v_MV2EBIT1Y = histWind.stream().mapToDouble(obj -> obj.getMV2EBIT1Y()).toArray();
							double[] v_MV2EBIT4Q = histWind.stream().mapToDouble(obj -> obj.getMV2EBIT4Q()).toArray();
							
							double riskPe1Y = RiskProb.calcRiskProb(v_pe1Y, v_pe1Y[0]);
							double riskPe4Q = RiskProb.calcRiskProb(v_pe4Q, v_pe4Q[0]);
							double riskPb = RiskProb.calcRiskProb(v_pb, v_pb[0]);
							
							double riskMV2EBIT1Y = RiskProb.calcRiskProb(v_MV2EBIT1Y, v_MV2EBIT1Y[0]);
							double riskMV2EBIT4Q = RiskProb.calcRiskProb(v_MV2EBIT4Q, v_MV2EBIT4Q[0]);
							
							//5Y窗口
							cal.setTime(toDate);
							cal.add(Calendar.YEAR, -5);
							fromDate = cal.getTime();
						
							List<StockDayView> histWind_5Y = stockDayViewWind.rangeOf(fromDate, toDate);
							double[] v_pe1Y_5Y = histWind_5Y.stream().mapToDouble(obj -> obj.getPe1Y()).toArray();
							double[] v_pe4Q_5Y = histWind_5Y.stream().mapToDouble(obj -> obj.getPe4Q()).toArray();
							double[] v_pb_5Y = histWind_5Y.stream().mapToDouble(obj -> obj.getPb()).toArray();
							
							double[] v_MV2EBIT1Y_5Y = histWind_5Y.stream().mapToDouble(obj -> obj.getMV2EBIT1Y()).toArray();
							double[] v_MV2EBIT4Q_5Y = histWind_5Y.stream().mapToDouble(obj -> obj.getMV2EBIT4Q()).toArray();
							
							double riskPe1Y_5Y = RiskProb.calcRiskProb(v_pe1Y_5Y, v_pe1Y_5Y[0]);
							double riskPe4Q_5Y = RiskProb.calcRiskProb(v_pe4Q_5Y, v_pe4Q_5Y[0]);
							double riskPb_5Y = RiskProb.calcRiskProb(v_pb_5Y, v_pb_5Y[0]);
							
							double riskMV2EBIT1Y_5Y = RiskProb.calcRiskProb(v_MV2EBIT1Y_5Y, v_MV2EBIT1Y_5Y[0]);
							double riskMV2EBIT4Q_5Y = RiskProb.calcRiskProb(v_MV2EBIT4Q_5Y, v_MV2EBIT4Q_5Y[0]);
							
							List<Object> record = new ArrayList<Object>();
							
							List<StockQuarterView> quarterFinanceHist = stockDayViewWind.financeChgHistWind.quarterFinanceHist;
							//最新财季
							String newFinQ = quarterFinanceHist.get(0).getReportYear()+"Q"+quarterFinanceHist.get(0).getReportQuarter();
							record.addAll(Arrays.asList(new Object[] {stockDayViewWind.get(0).getDate(), 
									//最新财季
									newFinQ,
									//
									symbol, name, hyname,
									shangshi_date, chengli_date,zuzhixingshi,zhuce_addr,jingyingfanwei,
									//风险水位
									stockDayViewWind.get(0).getPe1Y(), riskPe1Y, riskPe1Y_5Y,
									stockDayViewWind.get(0).getPe4Q(), riskPe4Q, riskPe4Q_5Y,
									stockDayViewWind.get(0).getPb(), riskPb, riskPb_5Y,
									//
									stockDayViewWind.get(0).getMV2EBIT1Y(), riskMV2EBIT1Y, riskMV2EBIT1Y_5Y,
									stockDayViewWind.get(0).getMV2EBIT4Q(), riskMV2EBIT4Q, riskMV2EBIT4Q_5Y
							}));
							
							//矩阵
							/*
							for (int i = 0; i < years; i++) {
								record.add(v_netprofit[i]);
								record.add(v_roe[i]);
								record.add(v_ganggan[i]);
							}
							*/
							
							//所得税
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(0).get("盈利质量"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(1).get("盈利质量"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(2).get("盈利质量"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(0).get("利润总额"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(1).get("利润总额"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(2).get("利润总额"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(0).get("所得税费用"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(1).get("所得税费用"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(2).get("所得税费用"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(0).get("所得税费用/利润总额"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(1).get("所得税费用/利润总额"));
							record.add(suodeshuiSatisfiedStocks.get(symbol).get(2).get("所得税费用/利润总额"));
							
							record.addAll(Arrays.asList(Arrays.copyOf(v_roe, years)));
							record.addAll(Arrays.asList(Arrays.copyOf(v_ganggan, years)));
							record.addAll(Arrays.asList(Arrays.copyOf(v_netprofit, years)));
							
							//
							record.addAll(Arrays.asList(Arrays.copyOf(v_totalAssets, years)));
							record.addAll(Arrays.asList(Arrays.copyOf(v_totalShareEquity, years)));
							
							//总资产增长率
							//double[] v_totalAssetsGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_totalAssets), -1),100);
							record.addAll(Doubles.asList(Arrays.copyOf(v_totalAssetsGrowthRate, years)));
							//净资产增长率
							//double[] v_totalShareEquityGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_totalShareEquity), -1),100);
							record.addAll(Doubles.asList(Arrays.copyOf(v_totalShareEquityGrowthRate, years)));
							
							//净利润增长率
							//@20180306 添加最新季度净利润增长率(同比)
							record.add(100*Functions.simR(quarterFinanceHist.get(0).getNetprofit(), quarterFinanceHist.get(4).getNetprofit()));
							//double[] v_netprofitGrowthRate = Functions.multiply(Functions.simR(ArrayUtils.toPrimitive(v_netprofit), -1),100);
							record.addAll(Doubles.asList(Arrays.copyOf(v_netprofitGrowthRate, years)));
							
							//@20180228
							//主营业务收入增长率 存在null
							//@20180306 添加最新季度主营业务收入增长率(同比)
							record.add(quarterFinanceHist.get(0).getMainBizIncomeGrowthRate());
							//Double[] v_mainBizIncomeGrowthRate = yearFinanceHist.stream().map(obj -> obj.getMainBizIncomeGrowthRate()).toArray(Double[]::new);
							//record.addAll(Doubles.asList());
							//record.addAll(Arrays.asList(ArrayUtils.subarray(v_mainBizIncomeGrowthRate, 0, years)));
							record.addAll(Doubles.asList(Arrays.copyOf(v_mainBizIncomeGrowthRate, years)));
							
							
							//EBIT增长率 存在null
							//@20180306 添加最新季度EBIT增长率(同比)
							record.add(100*Functions.simR(quarterFinanceHist.get(0).getEBIT(), quarterFinanceHist.get(4).getEBIT()));
							Double[]  v_EBIT = yearFinanceHist.stream().map(obj -> obj.getEBIT()).toArray(Double[]::new);
							Double[] v_EBITGrowthRate = Functions.multiply(Functions.simR(v_EBIT, -1),100);
							//record.addAll(Doubles.asList(Arrays.copyOf(v_EBITGrowthRate, years)));
							record.addAll(Arrays.asList(ArrayUtils.subarray(v_EBITGrowthRate, 0, years)));
							
							//@20180311
							//经营税增长率
							record.add(100*Functions.simR(quarterFinanceHist.get(0).getJingyingShui(), quarterFinanceHist.get(4).getJingyingShui()));
							Double[]  v_jingyingShui = yearFinanceHist.stream().map(obj -> obj.getJingyingShui()).toArray(Double[]::new);
							Double[] v_jingyingShuiGrowthRate = Functions.multiply(Functions.simR(v_jingyingShui, -1),100);
							record.addAll(Arrays.asList(ArrayUtils.subarray(v_jingyingShuiGrowthRate, 0, years)));
							//所得税增长率
							record.add(100*Functions.simR(quarterFinanceHist.get(0).getSuodeShui(), quarterFinanceHist.get(4).getSuodeShui()));
							Double[]  v_suodeShui = yearFinanceHist.stream().map(obj -> obj.getSuodeShui()).toArray(Double[]::new);
							Double[] v_suodeShuiGrowthRate = Functions.multiply(Functions.simR(v_suodeShui, -1),100);
							record.addAll(Arrays.asList(ArrayUtils.subarray(v_suodeShuiGrowthRate, 0, years)));
							
							
							if (classify == 1) {
								mingxingCases.add(record.toArray());
							} else if (classify == 2) {
								highNetprofitCases.add(record.toArray());
							} else if (classify == 3) {
								highRoeCases.add(record.toArray());
							} else if (classify == 4) {
								highGrowthCases.add(record.toArray());
							} else if (classify == 5) {
								lianghaoCases.add(record.toArray());
							} else if (classify == 6) {
								guanzhuCases.add(record.toArray());
							} else {
								
							}
						}


					} catch (Exception e) {
						logger.error("filter()出错: " + symbol, e);
					} finally {
						countDownLatch.countDown();

						logger.info(symbol + " 结束, 剩余: " + countDownLatch.getCount());
					}

				}
			});

			while (workQueue.size() > 90) {
				logger.info("waiting...");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// 等待所有任务执行结束
		try {

			countDownLatch.await();
			
			//排序
			sortList(mingxingCases, 4, 13);
			sortList(highNetprofitCases, 4, 13);
			sortList(highRoeCases, 4, 13);
			sortList(highGrowthCases, 4, 13);
			sortList(lianghaoCases, 4, 13);
			sortList(guanzhuCases, 4, 13);

			// 输出文件
			String outputFileName = "filters.output/ValueCases-"+years+"YMA-"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx";

			
			/*
			tableData.add(0, new String[] {"日期","代码", "名称", "行业",
					"上市日期","成立日期","组织形式","注册地址","经营范围",
					"PE1Y","PE4Q",
					"2016\n净利润","2016\nROE","2016\n杠杆率",
					"2015\n净利润","2015\nROE","2015\n杠杆率",
					"2014\n净利润","2014\nROE","2014\n杠杆率",
					"2013\n净利润","2013\nROE","2013\n杠杆率",
					"2012\n净利润","2012\nROE","2012\n杠杆率"
			});
			*/
			
			List<Object> headers = new ArrayList<Object>();
			//列:[0-24]
			headers.addAll(Arrays.asList(new String[] {"日期","最新财报","代码", "名称", "行业",
					"上市日期","成立日期","组织形式","注册地址","经营范围",
					"PE1Y","PE1Y\n风险"+years+"Y","PE1Y\n风险5Y",
					"PE4Q","PE4Q\n风险"+years+"Y","PE4Q\n风险5Y",
					"PB","PB\n风险"+years+"Y","PB\n风险5Y",
					//
					"MV2EBIT1Y","MV2EBIT1Y\n风险"+years+"Y","MV2EBIT1Y\n风险5Y",
					"MV2EBIT4Q","MV2EBIT4Q\n风险"+years+"Y","MV2EBIT4Q\n风险5Y"
			}));
			
			/*
			for (int i = 0; i < years; i++) {
				headers.add(yearsName[i]+"\nROE");
				headers.add(yearsName[i]+"\n杠杆率");
				headers.add(yearsName[i]+"\n净利润");
			}
			*/
			
			//列:[25-36]
			//实际年份不同股票可能不一致
			headers.add("0Y\n盈利质量");
			headers.add("-1Y\n盈利质量");
			headers.add("-2Y\n盈利质量");
			headers.add("0Y\n利润总额");
			headers.add("-1Y\n利润总额");
			headers.add("-2Y\n利润总额");
			headers.add("0Y\n所得税费用");
			headers.add("-1Y\n所得税费用");
			headers.add("-2Y\n所得税费用");
			headers.add("0Y\n所得税费用/利润总额");
			headers.add("-1Y\n所得税费用/利润总额");
			headers.add("-2Y\n所得税费用/利润总额");
			
			//
			List<String> yearsName = new ArrayList<String>();
			for (int i = 0; i < years; i++) {
				yearsName.add("-" + i + "Y");
			}
			
			//列:[37-]
			headers.addAll(yearsName.stream().map(obj -> obj + "\nROE").collect(Collectors.toList()) );
			headers.addAll(yearsName.stream().map(obj -> obj + "\n杠杆率").collect(Collectors.toList()) );
			headers.addAll(yearsName.stream().map(obj -> obj + "\n净利润").collect(Collectors.toList()) );
			//
			headers.addAll(yearsName.stream().map(obj -> obj + "\n总资产").collect(Collectors.toList()) );
			headers.addAll(yearsName.stream().map(obj -> obj + "\n净资产").collect(Collectors.toList()) );
			//成长性指标
			//列:[37+5*years ~ 37+7*years+5*(years+1)]
			headers.addAll(yearsName.stream().map(obj -> obj + "\n总资产增长率").collect(Collectors.toList()) );
			headers.addAll(yearsName.stream().map(obj -> obj + "\n净资产增长率").collect(Collectors.toList()) );
			//列:[37+7*years-]
			headers.add("最新财报\n净利润增长率");
			headers.addAll(yearsName.stream().map(obj -> obj + "\n净利润增长率").collect(Collectors.toList()) );
			headers.add("最新财报\n主营收入增长率");
			headers.addAll(yearsName.stream().map(obj -> obj + "\n主营收入增长率").collect(Collectors.toList()) );
			headers.add("最新财报\nEBIT增长率");
			headers.addAll(yearsName.stream().map(obj -> obj + "\nEBIT增长率").collect(Collectors.toList()) );
			headers.add("最新财报\n经营税增长率");
			headers.addAll(yearsName.stream().map(obj -> obj + "\n经营税增长率").collect(Collectors.toList()) );
			headers.add("最新财报\n所得税增长率");
			headers.addAll(yearsName.stream().map(obj -> obj + "\n所得税增长率").collect(Collectors.toList()) );
			
			//headers最后一个元素为样式集合
			//冻结窗格,列分组,为突出显示的列设置样式
			headers.add(new Object[] {
					//冻结窗格
					new int[]{5,1},
					//列分组
					new int[][]{{37,37+years-1},{37+years, 37+2*years-1},{37+2*years,37+3*years-1},{37+3*years,37+4*years-1},
						{37+4*years,37+5*years-1},{37+5*years,37+6*years-1},{37+6*years,37+7*years-1},{37+7*years,37+7*years+(years+1)-1},
						{37+7*years+(years+1),37+7*years+2*(years+1)-1},{37+7*years+2*(years+1),37+7*years+3*(years+1)-1},
						{37+7*years+3*(years+1),37+7*years+4*(years+1)-1},{37+7*years+4*(years+1),37+7*years+5*(years+1)-1}},
					//突出显示列
					new int[]{37, 37+years, 37+2*years,37+3*years,37+4*years,37+5*years,37+6*years,
					37+7*years,37+7*years+(years+1),37+7*years+2*(years+1),37+7*years+3*(years+1),37+7*years+4*(years+1)},
					//条件格式区域
					new Object[]{new int[][]{{11,12},{14,15},{17,18},{20,21},{23,24}}, 
							new int[][]{{37,37+years-1},{37+5*years,37+7*years+5*(years+1)}}},
					//条件规则阈值
					new Object[]{new double[]{0d,0.4d,1d}, new double[]{-30d,10d,50d}}
					
			});
			
			//
			mingxingCases.add(0, headers.toArray() );
			highNetprofitCases.add(0, headers.toArray() );
			highRoeCases.add(0, headers.toArray() );
			highGrowthCases.add(0, headers.toArray() );
			lianghaoCases.add(0, headers.toArray() );
			//
			guanzhuCases.add(0, headers.toArray() );


			// TODO: 市盈率，市净率的风险水位：a.根据日线样本排序计算下跌概率 b.根据值计算下跌空间，下跌/上涨空间比
			ExcelUtil.exportTables2Excel(new String[] {"明星","高成长","高ROE","高净利润","良好", "关注" }, 
					new List[] {mingxingCases,highNetprofitCases,highRoeCases,highGrowthCases,lianghaoCases,guanzhuCases}, 
					true, true,	outputFileName);

		} catch (Exception e) {
			logger.error("筛选出错", e);
		} finally {
			//
		}
	}
	
	/**
	 * 
	 * @param isChart
	 */
	public static void filterByQ(boolean isChart) {

		// List<String> symbols = new Vector<String>();
		List<Object[]> tableData = new Vector<Object[]>();

		String[] quarter = new String[20];
		
		final CountDownLatch countDownLatch = new CountDownLatch(allStockInfo.size());
		for (final String symbol : allStockInfo.keySet()) {

			executor.execute(new Runnable() {

				@Override
				public void run() {

					//String symbol = stk.getSymbol();
					try {

						StockDayViewWind stockDayViewWind = MemStatsStockDay.stats(symbol);
						if (stockDayViewWind == null || stockDayViewWind.size() < 240*5
								|| stockDayViewWind.financeChgHistWind == null
								|| stockDayViewWind.financeChgHistWind.quarterFinanceHist.size() < 20) {
							return;
						}
						/*
						if (stockDayViewWind == null 
								|| stockDayViewWind.quarterViewWind == null
								|| stockDayViewWind.quarterViewWind.size() < 20) {
							return;
						}*/
						
						StockQuarterViewWind stockQuarterViewWind = stockDayViewWind.financeChgHistWind;
						
						// 排除近5年存在亏损记录的公司，此处使用年报指标不使用4Q季度滚动指标，滚动指标容易出现负值
						List<StockQuarterView> quarterViewHist = stockQuarterViewWind.quarterFinanceHist;
						
						//String[] quarter = new String[20];
						Double[] v_netprofit4Q = new Double[20];
						Double[] v_roe4Q = new Double[20];
						Double[] v_ganggan4Q = new Double[20];
						for (int i = 0; i < 20; i++) {
							quarter[i] = String.format("%sY%sQ",quarterViewHist.get(i).getReportYear(),quarterViewHist.get(i).getReportQuarter());
							v_netprofit4Q[i] = quarterViewHist.get(i).getNetprofit4Q();
							v_roe4Q[i] = quarterViewHist.get(i).getWeightedroe4Q();
							v_ganggan4Q[i] = quarterViewHist.get(i).getTotalAssets()/quarterViewHist.get(i).getTotalShareEquity();
						}
						
						
						// 分类
						IExpressContext<String, Object> expressContext = new DefaultContext<String, Object>();
						expressContext.put("v_netprofit4Q", v_netprofit4Q);
						expressContext.put("v_roe4Q", v_roe4Q);

						Boolean isMatched = (Boolean) runner.execute(FilterRules.MINGXING_20Q, expressContext, null, false,
									false);

						if (isMatched) {
							/*
							//add headers
							List<String> headers = new ArrayList<String>();
							headers.add("日期");
							headers.add("代码");
							headers.add("名称");
							headers.add("行业");
							headers.add("PE1Y");
							headers.add("PE4Q");
							
							for (int i = 0; i < 20; i++) {
								headers.add(quarter[i]+"\n净利润");
								headers.add(quarter[i]+"\nROE");
								headers.add(quarter[i]+"\n杠杆率");
							}
							
							tableData.add(headers.toArray() );
							*/
							
							//add data
							//netprofit, roe, 杠杆率
							List<Object> record = new ArrayList<Object>();
							record.add(stockDayViewWind.get(0).getDate());
							record.add(symbol);
							String name = allStockInfo.get(symbol).get("name").toString();
							String hyname = allStockInfo.get(symbol).get("hyname").toString();
							record.add(name);
							record.add(hyname);
							record.add(stockDayViewWind.get(0).getPe1Y());
							record.add(stockDayViewWind.get(0).getPe4Q());
							
							for (int i = 0; i < 20; i++) {
								record.add(v_netprofit4Q[i]);
								record.add(v_roe4Q[i]);
								record.add(v_ganggan4Q[i]);
							}
							
							tableData.add(record.toArray());
						}


					} catch (Exception e) {
						logger.error("filter()出错: " + symbol, e);
					} finally {
						countDownLatch.countDown();

						logger.info(symbol + " 结束, 剩余: " + countDownLatch.getCount());
					}

				}
			});

			while (workQueue.size() > 90) {
				logger.info("waiting...");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// 等待所有任务执行结束
		try {

			countDownLatch.await();

			// 输出文件
			String outputFileName = "filters.output/MingxingCasesFilter-20Q-"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xls";

			//add headers
			//这里不严谨，不同股票日期不一定对齐
			List<String> headers = new ArrayList<String>();
			headers.add("日期");
			headers.add("代码");
			headers.add("名称");
			headers.add("行业");
			headers.add("PE1Y");
			headers.add("PE4Q");
			
			for (int i = 0; i < 20; i++) {
				headers.add(quarter[i]+"\n净利润");
				headers.add(quarter[i]+"\nROE");
				headers.add(quarter[i]+"\n杠杆率");
			}
			
			tableData.add(0, headers.toArray() );

			// TODO: 市盈率，市净率的风险水位：a.根据日线样本排序计算下跌概率 b.根据值计算下跌空间，下跌/上涨空间比
			ExcelUtil.exportTables2Excel(new String[] { "案例" }, new List[] { tableData }, true, true,
					outputFileName);

		} catch (Exception e) {
			logger.error("筛选出错", e);
		} finally {
			//
		}
	}
	
	
	/**
	 * 排序
	 * 中文用拼音排序
	 * @param records
	 * @return
	 */
	private static List<Object[]> sortList(List<Object[]> records, int firstSortIndex, int secondSortIndex) {
		List<FlexArray> list = FlexArray.toFlexArray(records);
		
        Collections.sort(list, new Comparator<FlexArray>() {
            @Override
            public int compare(FlexArray o1, FlexArray o2) {
                if (o1.getString(firstSortIndex).equals(o2.getString(firstSortIndex))) {
                    return o1.getDouble(secondSortIndex).compareTo(o2.getDouble(secondSortIndex));
                } else {
                	//return o1.getString(firstSortIndex).compareTo(o2.getString(firstSortIndex));
                	//中文拼音排序
                	Collator instance = Collator.getInstance(Locale.CHINA); 
                    return instance.compare(o1.getString(firstSortIndex), o2.getString(firstSortIndex));  
                }
            }
        });
        
        records.clear();
        records.addAll(FlexArray.toPrimArray(list));
        
        return FlexArray.toPrimArray(list);
    }
	
	public static class FlexArray {
		Object[] record = new Object[0];
		
		public static List<Object[]> toPrimArray(List<FlexArray> records) {
			List<Object[]> myRecords = new ArrayList<Object[]>();
			for (FlexArray record : records) {
				myRecords.add(record.record);
			} 
			return myRecords;
		}
		
		public static List<FlexArray> toFlexArray(List<Object[]> records) {
			List<FlexArray> mylist = new ArrayList<FlexArray>();
			for (Object[] record : records) {
				mylist.add(new FlexArray(record));
			}
			return mylist;
		}
		
		public FlexArray(Object[] record) {
			this.record = record;
		}
		
		String getString(int index) {
			return (String)record[index];
		}
		
		Double getDouble(int index) {
			return (Double)record[index];
		}
		
	}
	
	public static void main(String[] args) {

		logger.info("-------- 筛选开始 -------- ");

		// filter();
		//filter(true);
		//TODO: 添加股息率
		filterByY(10, true);
		
		//filterByY(8, true);
		
		filterByY(5, true);
		
		//filterByQ(true);
		//backfilter(true);
		/*
		try {
			filter(DateUtil.yyyyMMdd.get().parse("20110101"), DateUtil.yyyyMMdd.get().parse("20111231"), true);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		*/

		logger.info("-------- 筛选结束 -------- ");

	}
}
