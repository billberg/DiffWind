package com.diffwind.dao.util;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.diffwind.dao.mapper.CreateIndexMapper;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaStock;

public class TableUtil {
	
	private static Logger logger = Logger.getLogger(TableUtil.class);

	private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 1, TimeUnit.MINUTES, workQueue);
	private static Semaphore semaFinishedCount = new Semaphore(0);
	private static AtomicInteger finishedCount = new AtomicInteger();

	private static List<SinaStock> allStocks = null;
	private static SqlSessionFactory sqlSessionFactory = null;
	static {
		executor.allowCoreThreadTimeOut(true);
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources
					.getResourceAsReader(resource));
			
			SqlSession sqlSession = sqlSessionFactory.openSession(true);
			SinaStockMapper sinaStockMapper = sqlSession.getMapper(SinaStockMapper.class);
			allStocks = sinaStockMapper.selectAll();		
			sqlSession.close();
			
		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error",e);
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
	
	
}
