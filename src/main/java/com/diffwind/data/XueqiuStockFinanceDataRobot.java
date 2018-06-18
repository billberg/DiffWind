package com.diffwind.data;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.diffwind.dao.mapper.CreateIndexMapper;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.mapper.XueqiuStockFinanceMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.dao.model.XueqiuStockFinance;
import com.diffwind.stats.filters.MingxingCasesFilter;
import com.diffwind.util.DateUtil;
import com.diffwind.util.HttpUtil;

/**
 * 雪球股票财务数据
 * 改为增量下载
 * 
 * @author Billberg
 * 
 */
public class XueqiuStockFinanceDataRobot {

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
	
	private static  HashMap<String, String> httpHeaders = new HashMap<String, String>() {  
        {  
            put("Referer", "https://xueqiu.com");    
            put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:55.0) Gecko/20100101 Firefox/55.0");   
           
        }  
    };  
    
	//下载10年数据
	//private static String XUEQIU_FINANCE = "https://xueqiu.com/stock/f10/finmainindex.json?symbol={0}&page=1&size=40";
	//下载1年数据(size=4)，此处可调节size
	private static String XUEQIU_FINANCE = "https://xueqiu.com/stock/f10/finmainindex.json?symbol={0}&page=1&size={1}";
	private static String SOURCE_ENCODE = "utf-8";
	private static String OUTPUT_ENCODE = "utf-8";

	private static Logger logger = Logger.getLogger(XueqiuStockFinanceDataRobot.class);

	private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
	//性能瓶颈在网络，cpu空闲可以多开一些线程
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 1, TimeUnit.MINUTES, workQueue);
	//private static Semaphore semaFinishedCount = new Semaphore(0);
	//private static AtomicInteger finishedCount = new AtomicInteger();
	private static CountDownLatch countDownLatch = new CountDownLatch(0);

	//private static List<SinaStock> allStocks = null;

	private static SqlSessionFactory sqlSessionFactory = null;

	static {
		executor.allowCoreThreadTimeOut(true);
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),"simpleds");

			//SqlSession sqlSession = sqlSessionFactory.openSession(true);
			//SinaStockMapper sinaStockMapper = sqlSession.getMapper(SinaStockMapper.class);
			//allStocks = sinaStockMapper.selectAll();
			//sqlSession.close();
			
			//for test
//			allStocks.clear();
//			SinaStock stock = new SinaStock();
//			stock.setSymbol("SZ000921");
//			allStocks.add(stock);
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
	private static class ZengliangTask implements Runnable {

		private XueqiuStockFinance stkFinanceQ = null;

		public ZengliangTask(XueqiuStockFinance stkFinanceQ) {
			this.stkFinanceQ = stkFinanceQ;
		}

		public void run() {

			logger.info(stkFinanceQ.getSymbol());
			
			//SqlSession sqlSession = null;
			SqlSession batchSqlSession = null;
			try {
				Date lastReportDate = stkFinanceQ.getReportdate();
				//下个报表披露开始日
				Calendar cal = Calendar.getInstance();
				cal.setTime(lastReportDate);
				cal.add(Calendar.MONTH, 4);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				Date nextPublishdate = cal.getTime();
				
				if (new Date().before(nextPublishdate)) {
					logger.info(stkFinanceQ.getSymbol() + "@" + DateUtil.yyyyMMdd10.get().format(lastReportDate) + ": 财报数据已是最新");
					return;
				}

				
				//TODO: 要保证财报的连续性，下载一页不一定够
				String requestUrl = MessageFormat.format(XUEQIU_FINANCE, stkFinanceQ.getSymbol(), 4);

				//HttpUtil httpUtil = new HttpUtil();
				//httpUtil.setRequestUrl(requestUrl);
				//String response = httpUtil.doGet2();
				httpHeaders.put("Cookie", cookie);
				String response = HttpUtil.doGet(requestUrl,httpHeaders,10*1000);

				JSON.DEFFAULT_DATE_FORMAT = "yyyyMMdd";
				JSONObject jsonObj = JSON.parseObject(response);

				//financeRecords = null， sh600005退市
				JSONArray financeRecords = jsonObj.getJSONArray("list");
				if (financeRecords == null) {
					return;
				}

				//TODO: CHANGE TO BATCH
				//sqlSession = sqlSessionFactory.openSession(true);
				batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);

				XueqiuStockFinanceMapper xueqiuFinanceMapper = batchSqlSession.getMapper(XueqiuStockFinanceMapper.class);

				
				boolean isUpdate = false;
				for (int i = 0; i < financeRecords.size(); i++) {
					// XueqiuStockFinance xqFinance = financeRecords.getObject(i,
					// XueqiuStockFinance.class);
					JSONObject jsonObj2 = financeRecords.getJSONObject(i);
					XueqiuStockFinance xqFinance = jsonObj2.toJavaObject(XueqiuStockFinance.class);
					//增量更新财报
					Date reportdate = new SimpleDateFormat("yyyyMMdd").parse(jsonObj2.getString("reportdate"));
					if (reportdate.after( stkFinanceQ.getReportdate())) {
						isUpdate = true;
						
						xqFinance.setReportdate(reportdate);
						xqFinance.setSymbol(stkFinanceQ.getSymbol());
						xueqiuFinanceMapper.insert(xqFinance);
					}
				}
				
				if (isUpdate) {
					batchSqlSession.commit();
					batchSqlSession.clearCache();
					
					logger.info(String.format("<UPDATE>%s: 财报数据更新</UPDATE>", stkFinanceQ.getSymbol()) );
				}

			} catch (Exception e) {
				logger.error(stkFinanceQ.getSymbol() + "出错", e);
			} finally {
				if (batchSqlSession != null)
					batchSqlSession.close();
				
				countDownLatch.countDown();
				
				logger.info("剩余: " + countDownLatch.getCount());
			}

			
		}
	}
	
	
	private static class QuanliangTask implements Runnable {

		private String symbol = null;

		public QuanliangTask(String symbol) {
			this.symbol = symbol;
		}

		public void run() {

			logger.info(symbol);
			
			//SqlSession sqlSession = null;
			SqlSession batchSqlSession = null;
			try {
				int years = 20;
				String requestUrl = MessageFormat.format(XUEQIU_FINANCE, symbol, years*4);

				//HttpUtil httpUtil = new HttpUtil();
				//httpUtil.setRequestUrl(requestUrl);
				//String response = httpUtil.doGet2();
				httpHeaders.put("Cookie", cookie);
				String response = HttpUtil.doGet(requestUrl,httpHeaders,10*1000);

				JSON.DEFFAULT_DATE_FORMAT = "yyyyMMdd";
				JSONObject jsonObj = JSON.parseObject(response);
				//List<XueqiuStockFinance> records = JSON.parseArray(response, XueqiuStockFinance.class);

				//financeRecords = null， sh600005退市
				JSONArray financeRecords = jsonObj.getJSONArray("list");
				if (financeRecords == null) {
					return;
				}

				batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);

				XueqiuStockFinanceMapper xueqiuFinanceMapper = batchSqlSession.getMapper(XueqiuStockFinanceMapper.class);

				
				for (int i = 0; i < financeRecords.size(); i++) {
					// XueqiuStockFinance xqFinance = financeRecords.getObject(i,
					// XueqiuStockFinance.class);
					JSONObject jsonObj2 = financeRecords.getJSONObject(i);
					//XueqiuStockFinance test = financeRecords.getObject(i, XueqiuStockFinance.class);
					XueqiuStockFinance xqFinance = jsonObj2.toJavaObject(XueqiuStockFinance.class);
					Date reportdate = new SimpleDateFormat("yyyyMMdd").parse(jsonObj2.getString("reportdate"));
					
						xqFinance.setReportdate(reportdate);
						xqFinance.setSymbol(symbol);
						xueqiuFinanceMapper.insert(xqFinance);
					
				}
				
					batchSqlSession.commit();
					batchSqlSession.clearCache();
					
					logger.info(String.format("<UPDATE>%s: 财报数据更新</UPDATE>", symbol) );
				

			} catch (Exception e) {
				logger.error(symbol + "出错", e);
			} finally {
				if (batchSqlSession != null)
					batchSqlSession.close();
				
				countDownLatch.countDown();
				
				logger.info("剩余: " + countDownLatch.getCount());
			}

			
		}
	}


	private static void truncateTable(String tableName) {
		SqlSession sqlSession = sqlSessionFactory.openSession(true);

		TruncateTableMapper truncateTableMapper = sqlSession.getMapper(TruncateTableMapper.class);

		truncateTableMapper.truncateTable(tableName);

		sqlSession.close();

	}

	private static void createIndex(String sql) {
		SqlSession sqlSession = sqlSessionFactory.openSession(true);
		CreateIndexMapper createIndexMapper = sqlSession.getMapper(CreateIndexMapper.class);

		createIndexMapper.createIndex(sql);

		sqlSession.close();

	}

	
	private static String cookie;
	public static void downloadFinanceData(boolean isZengliang) {

		logger.info("-------- <xueqiu>下载主要财务指标开始 --------");

		if (!isZengliang) {
			//logger.info("#1::Truncate table xueqiu_stock_finance ...");
			truncateTable("xueqiu_stock_finance");
			
			countDownLatch = new CountDownLatch(MingxingCasesFilter.allStockInfo.size());
			
			cookie = getCookie();
			
			for (String symbol : MingxingCasesFilter.allStockInfo.keySet()) {
				
				executor.execute(new QuanliangTask(symbol));

				while (workQueue.size() > 90) {
					logger.info("waiting...");

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}

			}
		} else {

			//logger.info("#2::Xueqiu finance data downloading ... ");
			
			SqlSession sqlSession = sqlSessionFactory.openSession(true);
			XueqiuStockFinanceMapper stkFinMapper = sqlSession.getMapper(XueqiuStockFinanceMapper.class);
			List<XueqiuStockFinance> allStocks = stkFinMapper.selectAllStkLastReportdate();
			
			countDownLatch = new CountDownLatch(allStocks.size());
			
			cookie = getCookie();
	
			for (XueqiuStockFinance stkFinanceQ : allStocks) {
	
				executor.execute(new ZengliangTask(stkFinanceQ));
	
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
		//等待所有任务执行结束
		try {
			countDownLatch.await();
		
			logger.info("-------- 下载主要财务指标结束</xueqiu> --------");
			
		} catch (InterruptedException e) {
			logger.error("", e);
		}

		//
//		logger.info("#3::Create index...");
//		createIndex("idx1_xueqiu_finance on xueqiu_finance(symbol)");
//		createIndex("idx2_xueqiu_finance on xueqiu_finance(reportdate)");
		

	}

	
	public static String getCookie() {
		try {
			HttpClient client = HttpClients.createDefault();
			//requestUrl += "?" + EntityUtils.toString(new UrlEncodedFormEntity(requestParams),Consts.UTF_8); 
			
			HttpGet httpGet = new HttpGet("https://xueqiu.com/");
			
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(3000).setConnectTimeout(3000).build();//设置请求和传输超时时间
			httpGet.setConfig(requestConfig);
	
			httpGet.addHeader("Host", "xueqiu.com");
			httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
			httpGet.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
			httpGet.addHeader("Accept-Encoding","gzip, deflate, br");
			httpGet.addHeader("Connection", "keep-alive");
			//httpGet.addHeader("Cookie", "s=1adh11bwxe; xq_a_token=53e209bbcfd63cc4f4497d6d26df42da7977ff5a; Hm_lvt_1db88642e346389874251b5a1eded6e3=1450057065; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1450265829; __utma=1.1137084750.1450057065.1450242574.1450265829.10; __utmc=1; __utmz=1.1450057065.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); xqat=53e209bbcfd63cc4f4497d6d26df42da7977ff5a; xq_r_token=1e6effcb2447f456d684a00e2503cfcc481c9dcd; xq_is_login=1; u=1776991309; xq_token_expire=Fri%20Jan%2008%202016%2009%3A37%3A59%20GMT%2B0800%20(CST); snbim_minify=true; bid=d9601d32cacaec39ec8731be9526b977_ii5aliuu; webp=0");
			
			//logger.info("Request: " + requestUrl);
			// add request header
			// request.addHeader("User-Agent", USER_AGENT);
			HttpResponse response = client.execute(httpGet);
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("getCookie failed: "+response.getStatusLine().getStatusCode());
			}
			
			
			Header[] cookies = response.getHeaders("Set-Cookie");
			StringBuffer cookieStr = new StringBuffer();
			Map<String,String> cookieMap = new HashMap<String,String>();
			for (Header cookie : cookies) {
				String[] keyvalue = cookie.getValue().split("; *");
				for (String kv : keyvalue) {
					String[] k2v = kv.split("=");
					if (k2v.length == 2 && !cookieMap.containsKey(k2v[0])) {
						cookieMap.put(k2v[0], k2v[1]);
						cookieStr.append(kv).append("; ");
					}
				}
			}
			
			return cookieStr.toString();
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public static void main(String[] args) {

		//downloadFinanceData(true);
		
		//cookie = getCookie();
		//executor.execute(new QuanliangTask("sz000002"));
		
		downloadFinanceData(false);

	}

}