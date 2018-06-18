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
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.util.HttpUtil;

/**
 * 
 * 
 * @author Billberg
 * 
 */
public class SinaHqDataRobot {

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
	private static String REFERER = "http://vip.stock.finance.sina.com.cn/";

	// type=11/12 个股/指数
	// private static String RZRQ_SOURCE =
	// "https://xueqiu.com/stock/cata/stocklist.json?page={0}&size=100&order=desc&orderby=percent&type=11%2C12";
	// private static String RZRQ_SOURCE =
	// "https://xueqiu.com/stock/cata/stocklist.json?page={0}&size={1}&order=desc&orderby=percent&type=11";

	//注：不包含停牌股
	private static String SINA_HQ = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page={0}&num={1}&sort=changepercent&asc=0&node=hs_a&symbol=&_s_r_a=page";
	// private static String XUEQIU_SZA =
	// "https://xueqiu.com/stock/quote_order.json?page={0}&size={1}&order=desc&exchange=CN&stockType=sza&column=symbol%2Cname%2Ccurrent%2Cchg%2Cpercent%2Clast_close%2Copen%2Chigh%2Clow%2Cvolume%2Camount%2Cmarket_capital%2Cpe_ttm%2Chigh52w%2Clow52w%2Chasexist&orderBy=percent";

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

			truncateTableMapper.truncateTable("sina_stock");

			SinaStockMapper xqStockMapper = sqlSession.getMapper(SinaStockMapper.class);

				for (int p = 1; p <= PAGE_COUNT; p++) {
					String requestUrl = MessageFormat.format(SINA_HQ, p, PAGE_SIZE);

					HttpUtil httpUtil = new HttpUtil();
					httpUtil.setRequestUrl(requestUrl);
					String response = httpUtil.doGet();

					if (response == null || response.trim().equals("null")) {
						logger.info("sina hq data download finished");
						break;
					}
					
					JSONArray stocks = JSON.parseArray(response);

					if (stocks.size() == 0) {
						logger.info("sina hq data download finished");
						break;
					}
					
					for (int i = 0; i < stocks.size(); i++) {
						SinaStock xqStock = stocks.getObject(i,
						SinaStock.class);
						
						xqStockMapper.insert(xqStock);

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