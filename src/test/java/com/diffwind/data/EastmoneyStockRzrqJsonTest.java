package com.diffwind.data;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.diffwind.dao.mapper.EastmoneyStockRzrqJsonMapper;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.model.EastmoneyStockRzrqJson;
import com.diffwind.dao.model.SinaStock;

public class EastmoneyStockRzrqJsonTest {

	private static Logger logger = Logger.getLogger(EastmoneyStockRzrqJsonTest.class);

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
	
	
	//sh600104历史复权之后为负值，参考2006之前及2008
	@Test
	public void test() {
		
		SqlSession sqlSession = null;
		String symbol = "sh600104";
		try {
			sqlSession = sqlSessionFactory.openSession(true);
			EastmoneyStockRzrqJsonMapper eastmoneyStockRzrqJsonMapper = sqlSession.getMapper(EastmoneyStockRzrqJsonMapper.class);
			
		
			List<EastmoneyStockRzrqJson>  result = eastmoneyStockRzrqJsonMapper.selectAll();
			
			System.out.print(result.get(0).getRzrqData());
			
		} catch (Exception e) {
			logger.error("find出错: " + symbol, e);
		} finally {
			sqlSession.close();
		}
		
	}
}
