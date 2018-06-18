package com.diffwind.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.SinaZjhhangyeMapper;
import com.diffwind.dao.mapper.SinaZjhhangyeStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.dao.model.SinaZjhhangye;
import com.diffwind.dao.model.SinaZjhhangyeStock;
import com.diffwind.dao.model.XueqiuStockFinance;
import com.diffwind.data.EastmoneyStockRzrq.RzrqTask;
import com.diffwind.util.DateUtil;
import com.diffwind.util.HttpUtil;
import com.diffwind.util.ThreadPoolExecutorManager;

public class SinaStockCorpInfoDataRobot {

	private static HashMap<String, String> httpHeaders = new HashMap<String, String>() {
		{
			put("Host", "vip.stock.finance.sina.com.cn");
			put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:55.0) Gecko/20100101 Firefox/55.0");
			put("Accept", "*/*");
			put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
			put("Accept-Encoding", "gzip, deflate");
			// put("","");//Referer:
			// http://vip.stock.finance.sina.com.cn/corp/go.php/vISSUE_ShareBonus/stockid/600192.phtml
			// put("","");//Cookie: U_TRS1=0000003c.47e2d56.558ed05d.f566610f;
			// SINAGLOBAL=114.111.166.143_1464437891.928578;
			// ULV=1504526728801:19:1:1:218.241.251.191_1504335252.980128:1499176395689;
			// SGUID=1464437900034_35994469;
			// SUB=_2AkMvR8IddcNhrAFRnPgRxGLka4hH-jzEiebBAn7tJhMyAhgv7kczqSULxu2oWgqu7PXmG7QRnMRzBUPjww..;
			// SUBP=0033WrSXqPxfM72wWs9jqgMF55529P9D9W5gurQ4bdf-lwSp0uy.2y4J5JpVsgDDdJUAIs.VwJyydJU5IsvXdg8DdF4odcXt;
			// lxlrttp=1498824353;
			// FINA_V_S_2=sh600879,sz002419,sh601117,sz002517;
			// vjuids=-1af5ccf0e.154fb22550d.0.71d76d5ae9a94; vjlast=1496754587;
			// SCF=ArppfHALkaU_rwn-bUS7X1tBo9Owg9plsU6EUwqI0nqwt0Uo75fPPEB43cEd0wC6MzyLWEOl08sVbJEdscQ4_yQ.;
			// UOR=,,; Apache=218.241.251.191_1504335252.980128;
			// U_TRS2=000000cc.1ec123a.59ad417c.aae8d2cc;
			// FINANCE2=1e94a86ceb191d11de5c57a71e885941; _s_upa=10;
			// SINA_FINANCE=%3A%3A
			put("Connection", "keep-alive");
		}
	};

	// {0}-stock code
	private static String SINA_CORPINFO = "http://vip.stock.finance.sina.com.cn/corp/go.php/vCI_CorpInfo/stockid/{0}.phtml";

	private static String SOURCE_ENCODE = "GB2312";

	private static String OUTPUT_ENCODE = "UTF-8";
	static int TIMEOUT = 10;//seconds

	private static Logger logger = Logger.getLogger(SinaHqDataRobot.class);

	private static SqlSessionFactory sqlSessionFactory = null;

	static {
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),
					"simpleds");

			/*SqlSession sqlSession = sqlSessionFactory.openSession(true);
			SinaStockMapper sinaStockMapper = sqlSession.getMapper(SinaStockMapper.class);
			allStocks = sinaStockMapper.selectAll();
			sqlSession.close();
			*/

		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	//每次下载时间间隔ms
	private static int downloadInterval = 1100;
	public static void downloadSinaCorpInfo() {

		logger.info("-------- <sina>下载公司简介数据开始 --------");

		SqlSession sqlSession = null;

		try {

			List<String> stockSymbols = new ArrayList<String>();
			
			sqlSession = sqlSessionFactory.openSession(true);

			String sql = "select symbol from sina_stock except select symbol from sina_stock_corp_info";
			
			PreparedStatement preparedStatement = sqlSession.getConnection().
					prepareStatement(sql);
			
			
			ResultSet resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				stockSymbols.add(resultSet.getString(1)); 
			}
			
			final CountDownLatch countDownLatch = new CountDownLatch(stockSymbols.size());
			//ThreadPoolExecutorManager.poolSize = 1;
			for (String symbol : stockSymbols) {

				ThreadPoolExecutorManager.executor.execute(new Runnable() {

					@Override
					public void run() {

						try {
							downloadSinaCorpInfo(symbol);
						} catch(Throwable e) {
							logger.error("出错",e);
						} finally {
							countDownLatch.countDown();
							logger.info(symbol + "结束, 剩余: " + countDownLatch.getCount());
						}
					}

				});
				
				try {
					Thread.sleep(downloadInterval);
				} catch (InterruptedException e) {
					//e.printStackTrace();
				}
				
				while(ThreadPoolExecutorManager.workQueue.size() > 90) {
					logger.info("waiting...");
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}

			//等待所有任务执行结束
			try {
				countDownLatch.await();
				
			} catch (Exception e) {
				logger.error("出错: ", e);
			}
			

		} catch (Exception e) {
			logger.error("updateXueqiuStock出错", e);
		} finally {
			sqlSession.close();
		}

		logger.info("-------- 下载公司简介数据结束</sina> --------");
	}

	public static void downloadSinaCorpInfo(String symbol) {

		SqlSession sqlSession = null;

		try {

			//sqlSession = sqlSessionFactory.openSession(true);

			// 不要用Mapper了，用sql简单
			// SinaStockCorpInfoMapper stockCorpInfoMapper =
			// sqlSession.getMapper(SinaStockCorpInfoMapper.class);

			//

			String requestUrl = MessageFormat.format(SINA_CORPINFO, symbol.substring(2));

			/*
			HttpUtil httpUtil = new HttpUtil();
			httpUtil.setRequestUrl(requestUrl);
			String response = httpUtil.doGet();

			if (response == null || response.trim().equals("null")) {
				logger.error("SinaStockCorpInfo download failed:" + code);
				return;
			}
			*/
			
			URLConnection conn = null;
			
				URL url = new URL(requestUrl);
				conn = url.openConnection();//不建立连接
				conn.setConnectTimeout(TIMEOUT*1000);
				conn.setReadTimeout(TIMEOUT*1000);
				
			HtmlCleaner cleaner = new HtmlCleaner();
			InputStreamReader isr = new InputStreamReader(conn.getInputStream(), SOURCE_ENCODE);
			TagNode tagNode = cleaner.clean(isr);
			
			Object[] corpInfoNodes = tagNode
					.evaluateXPath("//table[@id='comInfo1']/tbody/tr");//
			
			/*
			 * "corp_name" varchar(60),
"shangshi_date" date,
"chengli_date" date,
"zuzhixingshi" varchar(30),
"history_names" varchar(200),
"zhuce_addr" varchar(200),
"jingying_addr" varchar(200),
"jianjie" text,
"jingyingfanwei" text
			 */
			String[] keys = {"公司名称","上市日期","成立日期","组织形式","证券简称更名历史","注册地址","办公地址","公司简介","经营范围"};
			Map<String,String> corpInfo = new HashMap<String,String>();
			for (int i = 0; i < corpInfoNodes.length; i++) {
				TagNode trNode = (TagNode) corpInfoNodes[i];
				StringBuilder line = new StringBuilder();
				TagNode[] tdNodes = trNode.getChildTags();

				for (int j = 0; j < tdNodes.length; j++) {
					/*
					if (tdNodes[j].getText().toString().trim()
									.contains("公司名称")) {

						corpInfo.put("公司名称", tdNodes[j+1].getText().toString().trim());
						
					} else if (tdNodes[j].getText().toString().trim()
							.contains("上市日期")) {

						corpInfo.put("上市日期", tdNodes[j+1].getText().toString().trim());
				
					} else if (tdNodes[j].getText().toString().trim()
							.contains("成立日期")) {

						corpInfo.put("成立日期", tdNodes[j+1].getText().toString().trim());
				
					} else if (tdNodes[j].getText().toString().trim()
							.contains("组织形式")) {

						corpInfo.put("组织形式", tdNodes[j+1].getText().toString().trim());
				
					} else if (tdNodes[j].getText().toString().trim()
							.contains("证券简称更名历史")) {

						corpInfo.put("证券简称更名历史", tdNodes[j+1].getText().toString().trim());
				
					} else if (tdNodes[j].getText().toString().trim()
							.contains("注册地址")) {

						corpInfo.put("注册地址", tdNodes[j+1].getText().toString().trim());
				
					}  else if (tdNodes[j].getText().toString().trim()
							.contains("办公地址")) {

						corpInfo.put("办公地址", tdNodes[j+1].getText().toString().trim());
				
					} else if (tdNodes[j].getText().toString().trim()
							.contains("公司简介")) {

						corpInfo.put("公司简介", tdNodes[j+1].getText().toString().trim());
				
					}  else if (tdNodes[j].getText().toString().trim()
							.contains("经营范围")) {

						corpInfo.put("经营范围", tdNodes[j+1].getText().toString().trim());
				
					}   
					*/ 
					
					for (int k = 0; k < keys.length; k++) {
						if (tdNodes[j].getText().toString().trim().startsWith(keys[k])) {

							corpInfo.put(keys[k], tdNodes[j+1].getText().toString().trim());
					
						} 
					}
				}
				
			}
			
			logger.info(corpInfo);
			
			sqlSession = sqlSessionFactory.openSession(true);
			
			String sql = "INSERT INTO sina_stock_corp_info(symbol,corp_name,shangshi_date,chengli_date,zuzhixingshi,"
					+ "history_names,zhuce_addr,bangong_addr,jianjie,jingyingfanwei) VALUES (?,?,?,?,?,?,?,?,?,?)";
			StringBuffer sb = new StringBuffer(sql);
			
			PreparedStatement preparedStatement = sqlSession.getConnection().
					prepareStatement(sql);
			
			preparedStatement.setString(1, symbol);
			for (int i = 0; i < keys.length; i++) {
				
				if (keys[i].contains("日期")) {
					preparedStatement.setDate(i+2, new java.sql.Date(DateUtil.yyyyMMdd10.get().parse(corpInfo.get(keys[i])).getTime()) );
				} else {
					preparedStatement.setString(i+2, corpInfo.get(keys[i]));
				}
			}
			preparedStatement.execute();

			
		} catch (Exception e) {
			logger.error("downloadSinaCorpInfo出错", e);
		} finally {
			sqlSession.close();
		}

	}

	public static void main(String[] args) {

		//downloadSinaCorpInfo("sz002508");
		
		//downloadSinaCorpInfo("sh600059");
		
		downloadSinaCorpInfo();
	}

}
