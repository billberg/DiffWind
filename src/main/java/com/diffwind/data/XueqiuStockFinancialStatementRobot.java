package com.diffwind.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.stats.filters.MingxingCasesFilter;

/**
 * 
 * 
 * @author Billberg
 * 
 */
public class XueqiuStockFinancialStatementRobot {

	private static String USER_AGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";
	// private static String COOKIE =
	// "U_TRS1=0000003c.f03c375f.529bf08e.308fd6b8;
	// U_TRS2=0000003c.f04b375f.529bf08e.a0acac2d;
	// UOR=,money.finance.sina.com.cn,;
	// ULV=1385951383960:2:2:2:124.205.103.60_1385951379.311157:1385951380642;
	// SINAGLOBAL=124.205.103.60_1385951379.311153;
	// Apache=124.205.103.60_1385951379.311157;
	// vjuids=-84458cc30.142b123c4eb.0.338a6f04bb15b8;
	// vjlast=1385951381.1385951381.10; _s_upa=1;
	// Suda_uid=124.205.103.60_1385951379.311159";
	private static String REFERER = "https://xueqiu.com";
	// private static String RZRQ_SOURCE =
	// "https://xueqiu.com/stock/f10/finmainindex.json?symbol={0}&page=1&size=40";

	//private static String XUEQIU_HIST = "https://xueqiu.com/S/{0}/historical.csv";
	private static String INCOME_STATEMENT = "http://api.xueqiu.com/stock/f10/incstatement.csv?symbol={0}&page=1&size=10000";

	//private static String OUTPUT_PATH = "xueqiu.day/";

	private static String SOURCE_ENCODE = "utf-8";
	private static String OUTPUT_ENCODE = "utf-8";

	private static Logger logger = Logger.getLogger(XueqiuStockFinancialStatementRobot.class);

	private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
	//性能瓶颈在网络，cpu空闲可以多开一些线程
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 1, TimeUnit.MINUTES, workQueue);
	
	private static CountDownLatch countDownLatch = new CountDownLatch(0);

	private static List<SinaStock> allStocks = null;

	private static SqlSessionFactory sqlSessionFactory = null;

	static {
		executor.allowCoreThreadTimeOut(true);
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),"simpleds");

			SqlSession sqlSession = sqlSessionFactory.openSession(true);
			SinaStockMapper sinaStockMapper = sqlSession.getMapper(SinaStockMapper.class);
			allStocks = sinaStockMapper.selectAll();
			sqlSession.close();
		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	/**
	 * Use HtmlCleaner
	 * 
	 * @author Billberg
	 * 
	 */
	private static class RobotTask implements Runnable {

		private String symbol = null;

		public RobotTask(String symbol) {
			//this.symbol = symbol.toUpperCase();
			this.symbol = symbol;
		}

		public void run() {

			logger.info(symbol);

			SqlSession sqlSession = null;
			BufferedReader br = null;

			try {

				sqlSession = sqlSessionFactory.openSession(true);

				String requestUrl = MessageFormat.format(INCOME_STATEMENT, symbol);

				URL url = new URL(requestUrl);
				URLConnection conn = url.openConnection();
				conn.setConnectTimeout(5 * 1000);// 10s
				conn.setReadTimeout(5 * 1000);
				conn.setRequestProperty("Host", "xueqiu.com");
				conn.setRequestProperty("User-Agent", USER_AGENT);
				conn.setRequestProperty("Referer", REFERER);
				conn.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				conn.setRequestProperty("Accept-Language","zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
				//conn.setRequestProperty("Accept-Encoding","gzip, deflate, br");
				conn.setRequestProperty("Connection", "keep-alive");

				//conn.setRequestProperty("Cookie",cookie);
				
				// StringBuffer cookieStr = new StringBuffer();
				// for (Header cookie : cookies) {
				// cookieStr.append(cookie.getValue());
				// }
				// conn.setRequestProperty("Cookie",cookieStr.toString());
				//

				br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				
				int lineNo = 0;
				String line = br.readLine();
				if (line == null || line.trim().isEmpty())
					return;

				//科目名
				String[] keys = line.split(",", -1);
				for (int i = 0; i < keys.length; i++) {
					keys[i] = keys[i].substring(keys[i].indexOf('"')+1, keys[i].lastIndexOf('"'));
					
					//科目名前的序号清除
					keys[i] = keys[i].replaceAll("一、|二、|三、|四、|五、|六、|七、|八、|九、|十、|\\(一\\)|\\(二\\)|\\(三\\)|\\(四\\)|\\(五\\)|\\(六\\)|\\(七\\)|\\(八\\)|\\(九\\)|\\(十\\)", "");
				
					//减:所得税 --> 所得税费用, 减:所得税费用 --> 所得税费用
					//if (keys[i].equals("减:所得税")) {
					if (keys[i].startsWith("减:所得税")) {
						keys[i] = "所得税费用";
					}
				}

				String sql = "INSERT INTO xueqiu_stock_income_statement_json VALUES (?::json)";
				
				while ((line = br.readLine()) != null) {
					
					String[] values = line.split(",", -1);
					//JSONObject jo = new JSONObject();
					//保存为有序，但不是插入顺序
					JSONObject jo = new JSONObject(16, true);
					jo.put("symbol", symbol);
					for (int i = 0; i < keys.length; i++) {
						if (!values[i].trim().isEmpty())
							jo.put(keys[i], values[i]);
					}
					
					PreparedStatement preparedStatement = sqlSession.getConnection().
							prepareStatement(sql);
					
					preparedStatement.setString(1, jo.toJSONString());
					preparedStatement.execute();
				}
				
		
			} catch (Exception e) {
				logger.error(symbol + "出错", e);
			} finally {

				countDownLatch.countDown();

				logger.info("剩余: " + countDownLatch.getCount());


				try {
					sqlSession.close();

					br.close();
				} catch (Exception e) {
					logger.error("socket/文件关闭异常", e);
				}

			}

		}
	}

	
	public static void downloadIncomeStatementData(boolean isZengliang) {

		logger.info("-------- <xueqiu>下载利润表数据开始 --------");
		
		if (!isZengliang) {
			
			logger.info("#1::Truncate table xueqiu_stock_income_statement_json ...");
			truncateTable("xueqiu_stock_income_statement_json");
			
			countDownLatch = new CountDownLatch(MingxingCasesFilter.allStockInfo.size());
			
		
			for (String symbol : MingxingCasesFilter.allStockInfo.keySet()) {
				
				executor.execute(new RobotTask(symbol));

				while (workQueue.size() > 90) {
					logger.info("waiting...");

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}

			}
	
		} 
		
		// wait all tasks to finish
		try {
			countDownLatch.await();
		
			logger.info("-------- 下载利润表数据结束</xueqiu> --------");
			
		} catch (InterruptedException e) {
			logger.error("", e);
		}


		//
//		logger.info("#3::Create index...");
//		createIndex("idx1_xueqiu_finance on xueqiu_finance(symbol)");
//		createIndex("idx2_xueqiu_finance on xueqiu_finance(reportdate)");

		

	}
	
	private static void truncateTable(String tableName) {
		SqlSession sqlSession = sqlSessionFactory.openSession(true);

		TruncateTableMapper truncateTableMapper = sqlSession.getMapper(TruncateTableMapper.class);

		truncateTableMapper.truncateTable(tableName);

		sqlSession.close();

	}

	public static void main(String[] args) {

		//downloadDayData();

		//cookie = XueqiuStockFinanceDataRobot.getCookie();
		
		//new RobotTask("SZ000069").run();
		downloadIncomeStatementData(false);
	}
}