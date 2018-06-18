package com.diffwind.data;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
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
import com.diffwind.dao.mapper.XueqiuStockShareschgMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.dao.model.XueqiuStockFinance;
import com.diffwind.dao.model.XueqiuStockShareschg;
import com.diffwind.stats.filters.MingxingCasesFilter;
import com.diffwind.util.DateUtil;
import com.diffwind.util.HttpUtil;

/**
 * 雪球股票股本变更数据
 * 
 * @author Billberg
 * 
 */
public class XueqiuStockShareschgDataRobot {

	private static  HashMap<String, String> httpHeaders = new HashMap<String, String>() {  
        {  
            put("Referer", "https://xueqiu.com");    
            put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:55.0) Gecko/20100101 Firefox/55.0");   
           
        }  
    };  
    
	//private static String XUEQIU_FINANCE = "https://xueqiu.com/stock/f10/finmainindex.json?symbol={0}&page=1&size=40";
	private static String XUEQIU_SHARESCHG = "https://xueqiu.com/stock/f10/shareschg.json?symbol={0}&page=1&size={1}";
	private static String TABLE_NAME = "xueqiu_stock_shareschg";
	
	private static String SOURCE_ENCODE = "utf-8";
	private static String OUTPUT_ENCODE = "utf-8";

	private static Logger logger = Logger.getLogger(XueqiuStockShareschgDataRobot.class);

	private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
	//性能瓶颈在网络，cpu空闲可以多开一些线程
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 1, TimeUnit.MINUTES, workQueue);
	//private static Semaphore semaFinishedCount = new Semaphore(0);
	//private static AtomicInteger finishedCount = new AtomicInteger();
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
			
			//for test
//			allStocks.clear();
//			SinaStock stock = new SinaStock();
//			stock.setSymbol("SZ300503");
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

		private String symbol = null;
		XueqiuStockShareschg lastShareschg = null;

		public ZengliangTask(XueqiuStockShareschg lastShareschg) {
			this.symbol = lastShareschg.getSymbol();
			this.lastShareschg = lastShareschg;
		}

		public void run() {

			logger.info(symbol);

			//SqlSession sqlSession = null;
			SqlSession batchSqlSession = null;

			try {
				
				String requestUrl = MessageFormat.format(XUEQIU_SHARESCHG, symbol, 40);

				//HttpUtil httpUtil = new HttpUtil();
				//httpUtil.setRequestUrl(requestUrl);
				//String response = httpUtil.doGet2();
				httpHeaders.put("Cookie", cookie);
				String response = HttpUtil.doGet(requestUrl,httpHeaders,10*1000);

				JSON.DEFFAULT_DATE_FORMAT = "yyyyMMdd";
				JSONObject jsonObj = JSON.parseObject(response);

				JSONArray financeRecords = jsonObj.getJSONArray("list");
				
				if (financeRecords == null)
					return;
				
				//sqlSession = sqlSessionFactory.openSession(true);
				batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);

				XueqiuStockShareschgMapper xqShareschgMapper = batchSqlSession.getMapper(XueqiuStockShareschgMapper.class);
				
				boolean isUpdate = false;
				for (int i = 0; i < financeRecords.size(); i++) {
					// XueqiuStockShareschg xqFinance = financeRecords.getObject(i,
					// XueqiuStockShareschg.class);
					//TODO: 设置日期反序列化格式
					JSONObject jsonObj2 = financeRecords.getJSONObject(i);
					XueqiuStockShareschg xqShareschg = jsonObj2.toJavaObject(XueqiuStockShareschg.class);
					Date publishdate = null;
					if (jsonObj2.getString("publishdate") != null)
						publishdate = new SimpleDateFormat("yyyyMMdd").parse(jsonObj2.getString("publishdate"));
					
					Date begindate = new SimpleDateFormat("yyyyMMdd").parse(jsonObj2.getString("begindate"));
					
					if (begindate.after( lastShareschg.getBegindate())) {
						isUpdate = true;
						
						xqShareschg.setPublishdate(publishdate);
						xqShareschg.setBegindate(begindate);
						xqShareschg.setSymbol(symbol);
	
						xqShareschgMapper.insert(xqShareschg);
					} else {
						break;
					}
				}
				
				
				if (isUpdate) {
					batchSqlSession.commit();
					batchSqlSession.clearCache();
					
					logger.info(String.format("<UPDATE>%s: 股本变更数据更新</UPDATE>", symbol) );
				} else {
					logger.info(symbol + ": 股本变更数据已是最新");
				}

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
				
				String requestUrl = MessageFormat.format(XUEQIU_SHARESCHG, symbol, 120);

				//HttpUtil httpUtil = new HttpUtil();
				//httpUtil.setRequestUrl(requestUrl);
				//String response = httpUtil.doGet2();
				httpHeaders.put("Cookie", cookie);
				String response = HttpUtil.doGet(requestUrl,httpHeaders,10*1000);

				JSON.DEFFAULT_DATE_FORMAT = "yyyyMMdd";
				JSONObject jsonObj = JSON.parseObject(response);

				JSONArray financeRecords = jsonObj.getJSONArray("list");
				
				if (financeRecords == null)
					return;
				
				//sqlSession = sqlSessionFactory.openSession(true);
				batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);

				XueqiuStockShareschgMapper xqShareschgMapper = batchSqlSession.getMapper(XueqiuStockShareschgMapper.class);
				
				for (int i = 0; i < financeRecords.size(); i++) {
					// XueqiuStockShareschg xqFinance = financeRecords.getObject(i,
					// XueqiuStockShareschg.class);
					//TODO: 设置日期反序列化格式
					JSONObject jsonObj2 = financeRecords.getJSONObject(i);
					XueqiuStockShareschg xqShareschg = jsonObj2.toJavaObject(XueqiuStockShareschg.class);
					Date publishdate = null;
					if (jsonObj2.getString("publishdate") != null)
						publishdate = new SimpleDateFormat("yyyyMMdd").parse(jsonObj2.getString("publishdate"));
					
					Date begindate = new SimpleDateFormat("yyyyMMdd").parse(jsonObj2.getString("begindate"));
				
						xqShareschg.setPublishdate(publishdate);
						xqShareschg.setBegindate(begindate);
						xqShareschg.setSymbol(symbol);
	
						xqShareschgMapper.insert(xqShareschg);
					
				}
				
				
					batchSqlSession.commit();
					batchSqlSession.clearCache();
					
					logger.info(String.format("<UPDATE>%s: 股本变更数据更新</UPDATE>", symbol) );
				

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
	public static void downloadShareschgData(boolean isZengliang) {

		logger.info("-------- <xueqiu>下载股本变更数据开始 --------");
		
		if (!isZengliang) {
			
			logger.info("#1::Truncate table xueqiu_stock_shareschg ...");
			truncateTable("xueqiu_stock_shareschg");
			
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
		
			SqlSession sqlSession = sqlSessionFactory.openSession(true);
			XueqiuStockShareschgMapper stkShareschgMapper = sqlSession.getMapper(XueqiuStockShareschgMapper.class);
			List<XueqiuStockShareschg> allStocks = stkShareschgMapper.selectAllStkLastShareschg();
			
			countDownLatch = new CountDownLatch(allStocks.size());
			
			cookie = getCookie();
	
			for (XueqiuStockShareschg xqStock : allStocks) {
	
				executor.execute(new ZengliangTask(xqStock));
	
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
		
			logger.info("-------- 下载股本变更数据结束</xueqiu> --------");
			
		} catch (InterruptedException e) {
			logger.error("", e);
		}


		//
//		logger.info("#3::Create index...");
//		createIndex("idx1_xueqiu_finance on xueqiu_finance(symbol)");
//		createIndex("idx2_xueqiu_finance on xueqiu_finance(reportdate)");

		

	}

	//TODO
	private static String getCookie() {
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

		//downloadShareschgData(true);
		
		downloadShareschgData(false);

	}

}