package com.diffwind.data;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.util.HttpUtil;
import com.diffwind.util.ThreadPoolExecutorManager;

/**
 * 东方财富融资融券历史数据
 * 尽量一次请求所有数据，不要分页获取数据，请求越多对服务器负载越大
 * 
 * @author Billberg
 * 
 */
public class EastmoneyStockRzrq {

	//增量获取股票融资融券数据
	private static String rzrq_home_page = "http://data.eastmoney.com/rzrq/";
	//private static String rzrq_home_json = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token={0}&st=rzrqyecz&sr=-1&p={1}&ps=20&js=var%20ycxStvou=%7bpages:(tp),data:(x)%7d&filter=(tdate=%27{2}%27)";
	//{0}: token, {1}: pageNo, {2}: pageSize, {3}: date
	private static String rzrq_home_json = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token={0}&st=rzrqyecz&sr=-1&p={1}&ps={2}&js=var%20ycxStvou=%7bpages:(tp),data:(x)%7d&filter=(tdate=%27{3}%27)";

	//全量获取某股票融资融券历史数据
	private static String stock_rzrq_page = "http://data.eastmoney.com/rzrq/detail/600104.html";
	//private static String stock_rzrq_json = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token={0}&filter=(scode=%27{1}%27)&st=tdate&sr=-1&p={2}&ps=50&js=var%20yKxpOsnj=%7bpages:(tp),data:(x)%7d";
	//{0}: token, {1}: scode, {2}: pageNo, {3}: pageSize
	private static String stock_rzrq_json = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token={0}&filter=(scode=%27{1}%27)&st=tdate&sr=-1&p={2}&ps={3}&js=var%20yKxpOsnj=%7bpages:(tp),data:(x)%7d";

	//private static String rzrq_json = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token=%s&filter=(scode='%s')&st=tdate&sr=-1&p=1&ps=50&js=var yKxpOsnj={pages:(tp),data:(x)}";

	private static String SOURCE_ENCODE = "utf-8";
	private static String OUTPUT_ENCODE = "utf-8";
	
	//2017-10-24T00:00:00
	private static String date_format = "yyyy-MM-dd'T'HH:mm:ss";

	private static Logger logger = Logger.getLogger(EastmoneyStockRzrq.class);
	
	private static SqlSessionFactory sqlSessionFactory = null;
	// private static SqlSessionFactory batchSqlSessionFactory = null;

	private static AtomicInteger finishedNum = new AtomicInteger();

	private static List<SinaStock> allStocks = null;
	
	static {
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
	

	private static String token = "";
	private static CountDownLatch countDownLatch = new CountDownLatch(0);
	public static void quanliangUpdate() {
		final List<String> scodeList = new ArrayList<String>();
		//String token = "";
		try {
			String response = HttpUtil.doGet(rzrq_home_page, null, 10*1000);
			//抓出token
			//抽取出dataurl
			//dataurl: "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token=70f12f2f4f091e459a279469fe49eca5&filter=(scode='600104')&st={sortType}&sr={sortRule}&p={page}&ps={pageSize}&js=var {jsname}={pages:(tp),data:(x)}{param}"
			Pattern pat1 = Pattern.compile("RZRQ_DETAIL_NJ&token=(.*?)&");
			Matcher matcher4 = pat1.matcher(response);
			if (!matcher4.find()) {
				logger.error("融资融券页面下载失败");
				return;
			}
			
			token = matcher4.group(1);
			
			logger.info("token=" + token);
			
			//TODO
			//下载最后一个交易日的融资融券记录
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			calendar.add(Calendar.DAY_OF_MONTH, -2);
			Date yesterday = calendar.getTime();
			String date = new SimpleDateFormat(date_format).format(DateUtils.truncate(yesterday, Calendar.DATE));
			
			/*
			int pageNo = 1;
			int pages = 1;
			for (pageNo = 1; pageNo <= pages; pageNo++) {
				String jsonUrl = MessageFormat.format(rzrq_home_json, token, pageNo, date);
				
				String jsonResponse = HttpUtil.doGet(jsonUrl, null, 10*1000);
				
				jsonResponse = jsonResponse.substring(jsonResponse.indexOf('{'), jsonResponse.lastIndexOf('}')+1);
				
				JSONObject rzrqJson = JSON.parseObject(jsonResponse);
				pages = rzrqJson.getIntValue("pages");
				
				logger.info("pages: " + pages + ", pageNo: " + pageNo);
			
				for (int i = 0; i < rzrqJson.getJSONArray("data").size(); i++) {
					String scode = rzrqJson.getJSONArray("data").getJSONObject(i).getString("scode");
					scodeList.add(scode);
				}
			}
			
			*/
			
			//默认5000被格式化为5,000
			String jsonUrl = MessageFormat.format(rzrq_home_json, token, 1, 5000+"", date);
			
			String jsonResponse = HttpUtil.doGet(jsonUrl, null, 10*1000);
			
			jsonResponse = jsonResponse.substring(jsonResponse.indexOf('{'), jsonResponse.lastIndexOf('}')+1);
			
			JSONObject rzrqJson = JSON.parseObject(jsonResponse);
			
			int pages = rzrqJson.getIntValue("pages");
			
			logger.info("pages: " + pages );
			logger.info("融资融券股票数: " + rzrqJson.getJSONArray("data").size());
			//assert pages == 1;
		
			for (int i = 0; i < rzrqJson.getJSONArray("data").size(); i++) {
				String scode = rzrqJson.getJSONArray("data").getJSONObject(i).getString("scode");
				scodeList.add(scode);
			}

		} catch (Exception e) {
			logger.error("find出错: ", e);
		} finally {
			//
		}
		
		if (scodeList.isEmpty()) {
			return;
		}
		
		//清表
		logger.info("清表: eastmoney_stock_rzrq_json");
		
		truncateTable("eastmoney_stock_rzrq_json");
		
		logger.info("-------- 下载个股融资融券历史数据 --------");
		
		
		quanliangUpdate(token, scodeList);
		
	}
	
	public static void quanliangUpdate(String token, List<String> scodeList) {
				
		countDownLatch = new CountDownLatch(scodeList.size());
				
				List<String> failCodes = new Vector<String>();
				for (final String scode : scodeList) {
					
					ThreadPoolExecutorManager.executor.execute(new RzrqTask(token, scode));
					
					while(ThreadPoolExecutorManager.workQueue.size() > 90) {
						logger.info("waiting...");
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				
				}
				
				//
				//等待所有任务执行结束
				try {
					countDownLatch.await();
				
					logger.info("-------- 下载个股融资融券历史数据@结束 --------");
					
					if (!failCodes.isEmpty()) {
						logger.info("下载失败代码: " + failCodes);
					}
					
				} catch (Exception e) {
					logger.error("出错: ", e);
				}
				
	}
	
	public static void zengliangUpdate() {
		SqlSession sqlSession = null;
		try {
			String response = HttpUtil.doGet(rzrq_home_page, null, 10*1000);
			//抓出token
			//抽取出dataurl
			//dataurl: "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token=70f12f2f4f091e459a279469fe49eca5&filter=(scode='600104')&st={sortType}&sr={sortRule}&p={page}&ps={pageSize}&js=var {jsname}={pages:(tp),data:(x)}{param}"
			Pattern pat1 = Pattern.compile("RZRQ_DETAIL_NJ&token=(.*?)&");
			Matcher matcher4 = pat1.matcher(response);
			if (!matcher4.find()) {
				logger.error("融资融券页面下载失败");
				return;
			}
			
			String token = matcher4.group(1);
			
			logger.info("token=" + token);
			
			sqlSession = sqlSessionFactory.openSession(true);
			
			String date = new SimpleDateFormat(date_format).format(DateUtils.truncate(new Date(), Calendar.DATE));
			int pageNo = 1;
			int pages = 1;
			for (pageNo = 1; pageNo <= pages; pageNo++) {
				String jsonUrl = MessageFormat.format(rzrq_home_json, token, pageNo, date);
				
				String jsonResponse = HttpUtil.doGet(jsonUrl, null, 10*1000);
				
				jsonResponse = jsonResponse.substring(jsonResponse.indexOf('{'), jsonResponse.lastIndexOf('}')+1);
				
				JSONObject rzrqJson = JSON.parseObject(jsonResponse);
				pages = rzrqJson.getIntValue("pages");
				
				logger.info("pages: " + pages + ", pageNo: " + pageNo);
				
				String sql = "INSERT INTO eastmoney_stock_rzrq_json VALUES (?::json)";
				StringBuffer sb = new StringBuffer(sql);
				if (rzrqJson.getJSONArray("data").size() > 0) {
					for (int i = 1; i < rzrqJson.getJSONArray("data").size(); i++) {
						sb.append(",(?::json)");
					}
				}
				sql = sb.toString();
				
				PreparedStatement preparedStatement = sqlSession.getConnection().
						prepareStatement(sql);
				
				for (int i = 0; i < rzrqJson.getJSONArray("data").size(); i++) {
					String json = rzrqJson.getJSONArray("data").get(i).toString();
					preparedStatement.setString(i+1, json);
				}
				preparedStatement.execute();
			}
			

		} catch (Exception e) {
			logger.error("find出错: ", e);
		} finally {
			sqlSession.close();
		}

	}
	
	public static void zengliangUpdate(Date txnDate) {
		SqlSession sqlSession = null;
		try {
			String response = HttpUtil.doGet(rzrq_home_page, null, 10*1000);
			//抓出token
			//抽取出dataurl
			//dataurl: "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?type=RZRQ_DETAIL_NJ&token=70f12f2f4f091e459a279469fe49eca5&filter=(scode='600104')&st={sortType}&sr={sortRule}&p={page}&ps={pageSize}&js=var {jsname}={pages:(tp),data:(x)}{param}"
			Pattern pat1 = Pattern.compile("RZRQ_DETAIL_NJ&token=(.*?)&");
			Matcher matcher4 = pat1.matcher(response);
			if (!matcher4.find()) {
				logger.error("融资融券页面下载失败");
				return;
			}
			
			String token = matcher4.group(1);
			
			logger.info("token=" + token);
			
			sqlSession = sqlSessionFactory.openSession(true);
			
			String date = new SimpleDateFormat(date_format).format(DateUtils.truncate(txnDate, Calendar.DATE));
			int pageNo = 1;
			int pages = 1;
			for (pageNo = 1; pageNo <= pages; pageNo++) {
				String jsonUrl = MessageFormat.format(rzrq_home_json, token, pageNo, date);
				
				String jsonResponse = HttpUtil.doGet(jsonUrl, null, 10*1000);
				
				jsonResponse = jsonResponse.substring(jsonResponse.indexOf('{'), jsonResponse.lastIndexOf('}')+1);
				
				JSONObject rzrqJson = JSON.parseObject(jsonResponse);
				pages = rzrqJson.getIntValue("pages");
				
				logger.info("pages: " + pages + ", pageNo: " + pageNo);
			
				
				String sql = "INSERT INTO eastmoney_stock_rzrq_json VALUES (?::json)";
				StringBuffer sb = new StringBuffer(sql);
				if (rzrqJson.getJSONArray("data").size() > 0) {
					for (int i = 1; i < rzrqJson.getJSONArray("data").size(); i++) {
						sb.append(",(?::json)");
					}
				}
				sql = sb.toString();
				
				PreparedStatement preparedStatement = sqlSession.getConnection().
						prepareStatement(sql);
				
				for (int i = 0; i < rzrqJson.getJSONArray("data").size(); i++) {
					String json = rzrqJson.getJSONArray("data").get(i).toString();
					preparedStatement.setString(i+1, json);
				}
				preparedStatement.execute();
			}
			

		} catch (Exception e) {
			logger.error("find出错: ", e);
		} finally {
			sqlSession.close();
		}

	}
	

	private static void truncateTable(String tableName) {
		SqlSession sqlSession = sqlSessionFactory.openSession(true);
		
		TruncateTableMapper truncateTableMapper = sqlSession.getMapper(TruncateTableMapper.class);

		truncateTableMapper.truncateTable(tableName);
		
		sqlSession.close();

	}
	
	
	
	static class RzrqTask implements Runnable {
		String token;
		String scode;
		
		public RzrqTask(String token, String scode) {
			this.token = token;
			this.scode = scode;
		}
		
		@Override
		public void run() {
			boolean isTryAgain = false;
			SqlSession sqlSession = null;
			try {
				sqlSession = sqlSessionFactory.openSession(true);
				
				/*
				int pageNo = 1;
				int pages = 1;
				for (pageNo = 1; pageNo <= pages; pageNo++) {
					String jsonUrl = MessageFormat.format(stock_rzrq_json, token, scode, pageNo);
					
					String jsonResponse = HttpUtil.doGet(jsonUrl, null, 10*1000);
					
					//logger.info("json: " + jsonResponse);
				
					jsonResponse = jsonResponse.substring(jsonResponse.indexOf('{'), jsonResponse.lastIndexOf('}')+1);
					
					JSONObject rzrqJson = JSON.parseObject(jsonResponse);
					pages = rzrqJson.getIntValue("pages");
					
					logger.info("pages: " + pages + ", pageNo: " + pageNo);
					
				
					String sql = "INSERT INTO eastmoney_stock_rzrq_json VALUES (?::json)";
					StringBuffer sb = new StringBuffer(sql);
					if (rzrqJson.getJSONArray("data").size() > 0) {
						for (int i = 1; i < rzrqJson.getJSONArray("data").size(); i++) {
							sb.append(",(?::json)");
						}
					}
					sql = sb.toString();
					
					PreparedStatement preparedStatement = sqlSession.getConnection().
							prepareStatement(sql);
					
					for (int i = 0; i < rzrqJson.getJSONArray("data").size(); i++) {
						String json = rzrqJson.getJSONArray("data").get(i).toString();
						preparedStatement.setString(i+1, json);
					}
					preparedStatement.execute();
				}
				
				*/
				//250*5
				String jsonUrl = MessageFormat.format(stock_rzrq_json, token, scode, 1, 1250);
				
				String jsonResponse = HttpUtil.doGet(jsonUrl, null, 10*1000);
				
				//logger.info("json: " + jsonResponse);
			
				jsonResponse = jsonResponse.substring(jsonResponse.indexOf('{'), jsonResponse.lastIndexOf('}')+1);
				
				JSONObject rzrqJson = JSON.parseObject(jsonResponse);
				
				logger.info("pages: " + rzrqJson.getIntValue("pages"));
			
				String sql = "INSERT INTO eastmoney_stock_rzrq_json VALUES (?::json)";
				StringBuffer sb = new StringBuffer(sql);
				if (rzrqJson.getJSONArray("data").size() > 0) {
					for (int i = 1; i < rzrqJson.getJSONArray("data").size(); i++) {
						sb.append(",(?::json)");
					}
				}
				sql = sb.toString();
				
				PreparedStatement preparedStatement = sqlSession.getConnection().
						prepareStatement(sql);
				
				for (int i = 0; i < rzrqJson.getJSONArray("data").size(); i++) {
					String json = rzrqJson.getJSONArray("data").get(i).toString();
					preparedStatement.setString(i+1, json);
				}
				preparedStatement.execute();
				
			
			} catch (ConnectException e) {
				logger.error("融资融券数据下载出错: " + scode, e);
				
				//重新提交任务执行
				logger.error("重新提交任务执行: " + scode);
				
				ThreadPoolExecutorManager.executor.execute(new RzrqTask(token, scode));
				isTryAgain = true;
				
			} catch(Exception e) {
				//回滚已提交数据
				String sql = "delete from eastmoney_stock_rzrq_json where rzrq_data->>'scode' = '%s'";
				sql = String.format(sql, scode);
			
				PreparedStatement preparedStatement;
				try {
					preparedStatement = sqlSession.getConnection().
							prepareStatement(sql);
					
					preparedStatement.execute();
					
					Thread.sleep(1000);
				} catch (SQLException e1) {
					logger.error("rzrq回滚失败", e1);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} finally {
				
				sqlSession.close();
				
				if (!isTryAgain) {
					countDownLatch.countDown();
				
					logger.info(scode + "结束, 剩余: " + countDownLatch.getCount());
				}
			}
			
		}
	}

	

	public static void main(String[] args) {
			//quanliangUpdate();
			
		String token = "70f12f2f4f091e459a279469fe49eca5";
		//先delete
		List<String> scodeList = Arrays.asList(new String[] {"510900", "601166", "601318", "000725", "601668", "600016", "600030", "002230", "600516", "000413", "600000", "002450", "600340", "300104", "601688", "600519", "601398", "000001", "600036", "000776", "600383", "000728", "600100", "600570", "002340", "600606", "600260", "600048", "601099", "600522", "000100", "600887", "000050", "601288", "601328", "601377", "601788", "000917", "600489", "300024", "300070", "601818", "600150", "002407", "000002", "300077", "600518", "600490", "000401", "002500", "601169", "000778", "002008", "600104", "601390", "000938", "600125", "601238", "600176", "300079", "600895", "000157", "002067", "601998", "601336", "600827", "002292", "600777", "000598", "600312", "600183", "600116", "002055", "600874", "000969", "601601", "000718", "300199", "600751", "300251", "600037", "600422", "000031", "000877", "002701", "603000", "000572", "601155", "000563", "600652", "002396", "002004", "600018", "000551", "002029", "600692", "600300", "600801", "601866", "600107", "002399", "002095", "002051", "600449", "600470", "600664", "002294", "600604", "600741", "600061", "600743", "000506", "600835", "300191", "600730", "002140", "002646", "000883", "002242", "600802", "601880", "600855", "000869", "601996", "002204", "600761", "000732", "600600", "512070"});
		
		quanliangUpdate(token, scodeList);
	}

}