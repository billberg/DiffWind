package com.diffwind.data;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.mapper.XueqiuStockMapper;
import com.diffwind.dao.model.XueqiuStock;
import com.diffwind.util.HttpUtil;

/**
 * 
 * 
 * @author Billberg
 * 
 */
public class XueqiuHqDataRobot {

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

	// type=11/12 个股/指数
	// private static String RZRQ_SOURCE =
	// "https://xueqiu.com/stock/cata/stocklist.json?page={0}&size=100&order=desc&orderby=percent&type=11%2C12";
	// private static String RZRQ_SOURCE =
	// "https://xueqiu.com/stock/cata/stocklist.json?page={0}&size={1}&order=desc&orderby=percent&type=11";

	//注：不包含停牌股
	private static String XUEQIU_HQ = "https://xueqiu.com/stock/quote_order.json?page={0}&size={1}&order=desc&exchange=CN&stockType={2}&column=symbol%2Cname%2Ccurrent%2Cchg%2Cpercent%2Clast_close%2Copen%2Chigh%2Clow%2Cvolume%2Camount%2Cmarket_capital%2Cpe_ttm%2Chigh52w%2Clow52w%2Chasexist&orderBy=percent";
	// private static String XUEQIU_SZA =
	// "https://xueqiu.com/stock/quote_order.json?page={0}&size={1}&order=desc&exchange=CN&stockType=sza&column=symbol%2Cname%2Ccurrent%2Cchg%2Cpercent%2Clast_close%2Copen%2Chigh%2Clow%2Cvolume%2Camount%2Cmarket_capital%2Cpe_ttm%2Chigh52w%2Clow52w%2Chasexist&orderBy=percent";

	private static String[] STOCK_TYPE = { "sha", "sza" };

	private static int PAGE_SIZE = 50;
	private static int PAGE_COUNT = 50;

	private static String SOURCE_ENCODE = "utf-8";
	private static String OUTPUT_ENCODE = "utf-8";

	private static Logger logger = Logger.getLogger(XueqiuHqDataRobot.class);

	private static SqlSessionFactory sqlSessionFactory = null;

	static {
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource));
		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	private static void updateXueqiuStock() {

		SqlSession sqlSession = null;

		try {

			sqlSession = sqlSessionFactory.openSession(true);

			TruncateTableMapper truncateTableMapper = sqlSession.getMapper(TruncateTableMapper.class);

			truncateTableMapper.truncateTable("xueqiu_stock");

			XueqiuStockMapper xqStockMapper = sqlSession.getMapper(XueqiuStockMapper.class);

			for (String stockType : STOCK_TYPE) {
				for (int p = 1; p <= PAGE_COUNT; p++) {
					String requestUrl = MessageFormat.format(XUEQIU_HQ, p, PAGE_SIZE, stockType);

					HttpUtil httpUtil = new HttpUtil();
					httpUtil.setRequestUrl(requestUrl);
					String response = httpUtil.doGet2();

					JSONObject jsonObj = JSON.parseObject(response);

					JSONArray stocks = jsonObj.getJSONArray("data");

					if (stocks.size() == 0) {
						logger.info(stockType + " hq data download finished");
						break;
					}
					
					for (int i = 0; i < stocks.size(); i++) {
						// XueqiuStock xqStock = stocks.getObject(i,
						// XueqiuStock.class);
						JSONArray hq = stocks.getJSONArray(i);

						String symbol = hq.getString(0);
						String name = hq.getString(1);

						XueqiuStock xqStock = new XueqiuStock();
						xqStock.setSymbol(symbol);
						xqStock.setName(name);

						xqStockMapper.insert(xqStock);

						// sqlSession.commit();

					}

				}

			}

		} catch (Exception e) {
			logger.error("updateXueqiuStock出错", e);
		} finally {
			sqlSession.close();
		}
	}

	public static void main(String[] args) {

		updateXueqiuStock();
	}

}