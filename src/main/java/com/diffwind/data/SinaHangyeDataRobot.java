package com.diffwind.data;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.diffwind.dao.mapper.SinaZjhhangyeMapper;
import com.diffwind.dao.mapper.SinaZjhhangyeStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaZjhhangye;
import com.diffwind.dao.model.SinaZjhhangyeStock;
import com.diffwind.util.HttpUtil;

public class SinaHangyeDataRobot {

	private static  HashMap<String, String> httpHeaders = new HashMap<String, String>() {  
        {  
            put("Host", "vip.stock.finance.sina.com.cn");    
            put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:55.0) Gecko/20100101 Firefox/55.0");   
            put("Accept","*/*");
            put("Accept-Language","zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
            put("Accept-Encoding","gzip, deflate");
            //put("","");//Referer: http://vip.stock.finance.sina.com.cn/corp/go.php/vISSUE_ShareBonus/stockid/600192.phtml
            //put("","");//Cookie: U_TRS1=0000003c.47e2d56.558ed05d.f566610f; SINAGLOBAL=114.111.166.143_1464437891.928578; ULV=1504526728801:19:1:1:218.241.251.191_1504335252.980128:1499176395689; SGUID=1464437900034_35994469; SUB=_2AkMvR8IddcNhrAFRnPgRxGLka4hH-jzEiebBAn7tJhMyAhgv7kczqSULxu2oWgqu7PXmG7QRnMRzBUPjww..; SUBP=0033WrSXqPxfM72wWs9jqgMF55529P9D9W5gurQ4bdf-lwSp0uy.2y4J5JpVsgDDdJUAIs.VwJyydJU5IsvXdg8DdF4odcXt; lxlrttp=1498824353; FINA_V_S_2=sh600879,sz002419,sh601117,sz002517; vjuids=-1af5ccf0e.154fb22550d.0.71d76d5ae9a94; vjlast=1496754587; SCF=ArppfHALkaU_rwn-bUS7X1tBo9Owg9plsU6EUwqI0nqwt0Uo75fPPEB43cEd0wC6MzyLWEOl08sVbJEdscQ4_yQ.; UOR=,,; Apache=218.241.251.191_1504335252.980128; U_TRS2=000000cc.1ec123a.59ad417c.aae8d2cc; FINANCE2=1e94a86ceb191d11de5c57a71e885941; _s_upa=10; SINA_FINANCE=%3A%3A
            put("Connection","keep-alive");
        }  
    };  
    
	// http://finance.sina.com.cn/stock/sl/#sinaindustry_1
	// http://finance.sina.com.cn/stock/sl/#industry_1
	// 证监会行业
	// http://money.finance.sina.com.cn/q/view/newFLJK.php?param=industry

	private static String USER_AGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";

	private static String REFERER = "http://finance.sina.com.cn/stock/";

	// 证监会行业
	// private static String ZJH_HANGYE =
	// "http://money.finance.sina.com.cn/q/view/newFLJK.php?param=industry";
	// 概念板块
	// private static String
	// GNBK="http://money.finance.sina.com.cn/q/view/newFLJK.php?param=class";
	// 行业板块、概念板块
	//private static String[] BANKUAI = { "industry", "class" };
	//private static String BANKUAI_URL = "http://money.finance.sina.com.cn/q/view/newFLJK.php?param={0}";

	//注：一次最多可以取100个num=100为上限
	//private static String ZJH_HANGYE_STOCK = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page={0}&num=100&sort=symbol&asc=1&node={1}";

	//使用新浪行业更好一些
	//单页最大数num=80
	private static String SINA_HANGYE = "http://vip.stock.finance.sina.com.cn/q/view/newSinaHy.php";
	private static String SINA_HANGYE_STOCK = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page={0}&num=80&sort=symbol&asc=0&node={1}&symbol=&_s_r_a=sort";

	private static int PAGE_SIZE = 80;
	private static int PAGE_COUNT = 50;

	private static String SOURCE_ENCODE = "utf-8";
	private static String OUTPUT_ENCODE = "utf-8";

	private static Logger logger = Logger.getLogger(SinaHqDataRobot.class);

	private static SqlSessionFactory sqlSessionFactory = null;

	static {
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),"simpleds");
		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	public static void updateSinaZjhhangye() {

		logger.info("-------- <sina>下载股票行业数据开始 --------");
		
		SqlSession sqlSession = null;

		try {

			sqlSession = sqlSessionFactory.openSession(true);

			TruncateTableMapper truncateTableMapper = sqlSession.getMapper(TruncateTableMapper.class);

			truncateTableMapper.truncateTable("sina_zjhhangye");
			truncateTableMapper.truncateTable("sina_zjhhangye_stock");

			SinaZjhhangyeMapper sinaZjhhangyeMapper = sqlSession.getMapper(SinaZjhhangyeMapper.class);
			final SinaZjhhangyeStockMapper sinaZjhhangyeStockMapper = sqlSession.getMapper(SinaZjhhangyeStockMapper.class);

			//请求计数，统计反爬虫策略
			final AtomicInteger requestCount = new AtomicInteger(0);
			// 行业板块
			//for (String bk : BANKUAI) {
				//String requestUrl = MessageFormat.format(BANKUAI_URL, bk);

				String requestUrl = SINA_HANGYE;
				HttpUtil httpUtil = new HttpUtil();
				httpUtil.setRequestUrl(requestUrl);
				String response = httpUtil.doGet();
				
				//该处不计数
				//logger.info("请求计数: " + requestCount.incrementAndGet() );

				if (response == null || response.trim().equals("null")) {
					throw new RuntimeException("sina zjh hangye data download failed");
				}

				String hangyeJson = response.split("=")[1];
				final JSONObject hangye = JSON.parseObject(hangyeJson);

				for (String code : hangye.keySet()) {
					if (code.trim().isEmpty())
						continue;

					SinaZjhhangye hy = new SinaZjhhangye();
					hy.setCode(code);
					String name = hangye.getString(code).split(",")[1];
					hy.setName(name);
					sinaZjhhangyeMapper.insert(hy);

				}

				//
				final CountDownLatch countDownLatch = new CountDownLatch(hangye.size());
				for (final String hycode : hangye.keySet()) {
					if (hycode.trim().isEmpty()) {
						countDownLatch.countDown();
						continue;
					}
					
					
					new Thread( new Runnable() {
					
						boolean isFinished = false;
						
						@Override
						public void run() {
							// 
							int page = 1;
							while(!isFinished) { 
								//String requestUrl = MessageFormat.format(ZJH_HANGYE_STOCK, page,hycode);
								String requestUrl = MessageFormat.format(SINA_HANGYE_STOCK, page,hycode);
								page++;
		
								HttpUtil httpUtil = new HttpUtil();
								httpUtil.setRequestUrl(requestUrl);
								String response = httpUtil.doGet();
								
								logger.info("请求计数: " + requestCount.incrementAndGet() );
								/*if (requestCount.get() % 50 == 0) {
									try {
										Thread.sleep(10*1000);
									} catch (InterruptedException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
		
								}*/
								
								if (response == null || response.trim().equals("null")) {
									logger.error("sina hangye stock data download failed:" + hycode);
									return;
								}
		
								JSONArray stocks = JSON.parseArray(response);
		
								if (stocks.size() == 0) {
									logger.error("sina hangye stock data download failed:" + hycode);
									return;
								}
		
								String hyname = hangye.getString(hycode).split(",")[1];
								for (int i = 0; i < stocks.size(); i++) {
									SinaZjhhangyeStock stock = stocks.getObject(i, SinaZjhhangyeStock.class);
									stock.setHycode(hycode);
									stock.setHyname(hyname);
		
									sinaZjhhangyeStockMapper.insert(stock);
								}
								
								if (stocks.size() < 80)
									isFinished = true;
								
								/*
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}*/
							}
							
							countDownLatch.countDown();
						}
					
					}).start();

				}
				
				countDownLatch.await();

			//}

		} catch (Exception e) {
			logger.error("updateXueqiuStock出错", e);
		} finally {
			sqlSession.close();
		}
		
		logger.info("-------- 下载股票行业数据结束</sina> --------");
	}
	

	public static void main(String[] args) {

		updateSinaZjhhangye();
	}

}
