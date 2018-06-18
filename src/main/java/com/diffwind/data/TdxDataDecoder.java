package com.diffwind.data;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.diffwind.dao.mapper.CreateIndexMapper;
import com.diffwind.dao.mapper.SinaStockMapper;
import com.diffwind.dao.mapper.TdxStockDayMapper;
import com.diffwind.dao.mapper.TruncateTableMapper;
import com.diffwind.dao.model.SinaStock;
import com.diffwind.dao.model.TdxStockDay;

/**
 * 通达信日线数据解码
 * 
 * @author Billberg
 * 
 */
public class TdxDataDecoder {
	private static Logger logger = Logger.getLogger(TdxDataDecoder.class);

	// Windows
	// private static String shDayPath = "C:/new_zszq/vipdoc/sh/lday/";
	// private static String szDayPath = "C:/new_zszq/vipdoc/sz/lday/";
	
	//private static String shDayPath = "/Users/zhangjz/DiffWind.data/vipdoc2006-2015/sh/lday/";
	//private static String szDayPath = "/Users/zhangjz/DiffWind.data/vipdoc2006-2015/sz/lday/";
	private static String shDayPath = "/Users/zhangjz/DiffWind.data/vipdoc/sh/lday/";
	private static String szDayPath = "/Users/zhangjz/DiffWind.data/vipdoc/sz/lday/";

	private static SqlSessionFactory sqlSessionFactory = null;
	
	//通达信指数代码
	private static  HashMap<String, String> tdxIndex = new HashMap<String, String>() {  
			{  
	            put("sh999999", "上证指数");    
	            put("sz399001", "深证成指"); 
	            put("sz399005", "中小板指"); 
	            put("sz399006", "创业板指"); 
	            put("sh000300", "沪深300"); 
	        }  
	};  
	
	private static boolean isIncludingIndex = true;
	    
	// symbol: sh600000
	private static List<String> allStockSymbols = new ArrayList<String>();
	static {
		// SqlSessionFactory sessionFactory = null;
		String resource = "mybatis-config.xml";
		try {
			//加载配置文件
			ClassLoader classLoader = TdxDataDecoder.class.getClassLoader();
			InputStream is = classLoader.getResourceAsStream("config.properties");
			BufferedReader bf = new BufferedReader(new InputStreamReader(is,"utf-8"));
			Properties config = new Properties();
			config.load(bf);
			
			shDayPath = config.getProperty("sh.day.path");
			szDayPath = config.getProperty("sz.day.path");
			
			//mybatis
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource),
					"batchds");

			SqlSession sqlSession = sqlSessionFactory.openSession(true);
			SinaStockMapper sinaStockMapper = sqlSession.getMapper(SinaStockMapper.class);
			List<SinaStock> allStocks = sinaStockMapper.selectAll();
			
			sqlSession.close();
			
			for (SinaStock stk : allStocks) {
				allStockSymbols.add(stk.getSymbol());
			}
			
			//添加指数代码
			if (isIncludingIndex) {
				allStockSymbols.addAll(tdxIndex.keySet());
			}
			
		} catch (IOException e) {
			logger.error("mybatis config error", e);
			throw new RuntimeException("mybatis config error", e);
		}
	}

	

	// private static HashMap<String, String> tdxBK = new HashMap<String,
	// String>();
	// private static HashMap<String, String> tdxIndex = new HashMap<String,
	// String>();
	// private static HashMap<String, String> tdxStk = new HashMap<String,
	// String>();

	static int THREADS = 2;

	public static void start(int fromYear, int toYear) {

		long startTime = System.nanoTime();
		
		logger.info("-------- <tdx>日线数据解码开始 --------");

		// getSessionFactory();

		String partition = fromYear + "-" + toYear;
		String partitionTable = "tdx_stock_day_" + partition;

		logger.info("Truncate table...");
		SqlSession sqlSession = sqlSessionFactory.openSession(true);

		TruncateTableMapper truncateTableMapper = sqlSession.getMapper(TruncateTableMapper.class);

		// truncateTableMapper.truncateTable("tdx_index_day");

		truncateTableMapper.truncateTable(partitionTable);

		logger.info("Drop index...");
		CreateIndexMapper createIndexMapper = sqlSession.getMapper(CreateIndexMapper.class);

		createIndexMapper.dropIndex("\"idx1_" + partitionTable + "\"");
		createIndexMapper.dropIndex("\"idx2_" + partitionTable + "\"");
		// idxDayMapper.dropIndex();
		// stkDayMapper.dropIndex();

		sqlSession.close();

		logger.info("Truncate table finished.");

		logger.info("Load day data...");
		ConcurrentLinkedQueue<String> stockQueue = new ConcurrentLinkedQueue<String>();

		// tdxIndex = Util.getTdxIndex();
		// tdxBK = Util.getTdxBK();
		// tdxStk = Util.getTdxStk();

		// stockQueue.addAll(tdxIndex.keySet());
		// stockQueue.addAll(tdxBK.keySet());
		// stockQueue.addAll(tdxStk.keySet());

		stockQueue.addAll(allStockSymbols);

		// for test
		//stockQueue.add("sh600261");
		try {
			Thread[] threads = new Thread[THREADS];
			for (int i = 0; i < THREADS; i++) {
				threads[i] = new Thread(new RobotTask(stockQueue, fromYear, toYear));
				// th.setDaemon(false);
				threads[i].setName("TdxDecoder-TH" + i);
				threads[i].start();
			}

			for (int i = 0; i < THREADS; i++) {
				threads[i].join();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		logger.info("Load data finished.");

		logger.info("Create index...");
		// idxDayMapper.createIndex();
		// stkDayMapper.createIndex();

		// sqlSession.commit();
		// sqlSession.close();

		// create index
		logger.info("#3::Create index...");
		String index1 = "\"idx1_" + partitionTable + "\" on \"" + partitionTable + "\" (symbol)";
		String index2 = "\"idx2_" + partitionTable + "\" on \"" + partitionTable + "\" (date)";
		sqlSession = sqlSessionFactory.openSession(true);

		createIndexMapper = sqlSession.getMapper(CreateIndexMapper.class);

		createIndexMapper.createIndex(index1);
		createIndexMapper.createIndex(index2);

		// idxDayMapper.dropIndex();
		// stkDayMapper.dropIndex();

		sqlSession.close();

		logger.info("Create index finished.");

		logger.info("-------- 日线数据解码结束</tdx> --------");

		long currTime = System.nanoTime();
		logger.info("Time Cost: " + (currTime - startTime) / 1e6);
	}

	private static int readInt(byte[] value, int off) {
		byte[] b4 = { value[off + 3], value[off + 2], value[off + 1], value[off] };

		ByteBuffer buf = ByteBuffer.wrap(b4);
		return buf.getInt();
	}

	public static long readUnsignedInt(byte[] value, int off) {

		/*
		 * byte[] bytes = { value[off + 3], value[off + 2], value[off + 1],
		 * value[off] };
		 */

		byte[] bytes = { value[off], value[off + 1], value[off + 2], value[off + 3] };

		long b0 = ((long) (bytes[0] & 0xff));
		long b1 = ((long) (bytes[1] & 0xff)) << 8;
		long b2 = ((long) (bytes[2] & 0xff)) << 16;
		long b3 = ((long) (bytes[3] & 0xff)) << 24;
		return (long) (b0 | b1 | b2 | b3);
	}

	/*
	 * private static long readLong(byte[] value, int off) { byte[] b4 =
	 * {value[off + 3], value[off + 2], value[off + 1], value[off], value[off +
	 * 7], value[off + 6], value[off + 5], value[off+4] };
	 * 
	 * //ByteBuffer buf = ByteBuffer.wrap(value,off,8);
	 * //buf.order(ByteOrder.BIG_ENDIAN);
	 * 
	 * ByteBuffer buf = ByteBuffer.wrap(b4); return buf.getLong(); }
	 */

	public static long readLong(byte[] value, int off) {

		/*
		 * byte[] bytes = { value[off + 3], value[off + 2], value[off + 1],
		 * value[off] };
		 */

		byte[] bytes = { value[off], value[off + 1], value[off + 2], value[off + 3], value[off + 4], value[off + 5],
				value[off + 6], value[off + 7] };

		long b0 = ((long) (bytes[0] & 0xff));
		long b1 = ((long) (bytes[1] & 0xff)) << 8;
		long b2 = ((long) (bytes[2] & 0xff)) << 16;
		long b3 = ((long) (bytes[3] & 0xff)) << 24;
		long b4 = ((long) (bytes[4] & 0xff)) << 32;
		long b5 = ((long) (bytes[5] & 0xff)) << 40;
		long b6 = ((long) (bytes[6] & 0xff)) << 48;
		long b7 = ((long) (bytes[7] & 0xff)) << 56;
		return (long) (b0 | b1 | b2 | b3 | b4 | b5 | b6 | b7);
	}

	private static float readFloat(byte[] value, int off) {
		byte[] b4 = { value[off + 3], value[off + 2], value[off + 1], value[off] };
		ByteBuffer buf = ByteBuffer.wrap(b4);
		return buf.getFloat();
	}

	// TODO:
	private static class RobotTask implements Runnable {
		private ConcurrentLinkedQueue<String> stockQueue = null;

		private int fromYear, toYear;

		public RobotTask(ConcurrentLinkedQueue<String> queue, int fromYear, int toYear) {
			this.stockQueue = queue;
			this.fromYear = fromYear;
			this.toYear = toYear;
		}

		public void run() {

			while (!stockQueue.isEmpty()) {

				// symbol
				String symbol = stockQueue.remove();

				logger.info(symbol);
				logger.info("Left: " + stockQueue.size());

				long startTime = System.currentTimeMillis();

				// String outputFile = null;
				String dayFile = null;
				boolean isIdx = false;
				// STOCK
				// outputFile = outputPath + stockCode + ".csv";

				if (symbol.startsWith("sh")) {
					dayFile = shDayPath + symbol + ".day";
					// printBinaryFileInt2String(dayFile);
				} else if (symbol.startsWith("sz")) {
					dayFile = szDayPath + symbol + ".day";
					// printBinaryFileInt2String(dayFile);
				}
				
				//有退市的情况
				if (!new File(dayFile).exists() )
					continue;

				// file header: 46*4=184
				// day data: 42*4 = 168 (but only first 7*4=28 bytes used)

				String partition = fromYear + "-" + toYear;
				
				//1年日线数据
				List<Object[]> tsData1y = new ArrayList<Object[]>();
				BufferedInputStream bin = null;
				try {
					bin = new BufferedInputStream(new FileInputStream(dayFile));
					//DataInputStream din = new DataInputStream(bin);

					byte[] value = new byte[32];
					int year = 0, yearDays = 0;
					while ((bin.read(value)) != -1) {
						int iDate = readInt(value, 0);
						int iYear = iDate / 10000;
						
						if (iYear != year) {
							//logger.info("Year: " + year + " yearDays: " + yearDays);
							
							if (year >= fromYear && year <= toYear) {
								batchInsertPartition(partition, symbol, tsData1y);
								tsData1y = new ArrayList<Object[]>();
							}
							
							year = iYear;
							yearDays = 0;
						}
						
						yearDays++;

						//
						if (iYear < fromYear || iYear > toYear) {
							continue;
						}

						//String txnDate = Long.toString(ldate);
						/*
						 * String txnDate = sdate.substring(0, 4) + '-' +
						 * sdate.substring(4, 6) + '-' + sdate.substring(6, 8);
						 */

						// TODO:存int类型提升效率
						double open = readInt(value, 4) / 100d;
						double high = readInt(value, 8) / 100d;
						double low = readInt(value, 12) / 100d;
						double close = readInt(value, 16) / 100d;
						double amt = readFloat(value, 20);

						// 该字段指数与个股不同
						// 个股:vol=成交笔数,vol/100=成交手数
						// 指数:vol=成交手数
						// int vol = readInt(value, 24);
						long vol = readUnsignedInt(value, 24);
						if (vol < 0)
							logger.error(symbol + " " + iDate + " 数据错误: vol:" + vol);

						int reserved = readInt(value, 28);
						// long undefined = readUnsignedInt(value, 28);
						if (reserved < 0) {
							logger.info("reserved:" + Integer.toHexString(reserved));
							// logger.info(Integer.toBinaryString(65536));
							// logger.info(Integer.toBinaryString(100));

							logger.warn(symbol + " " + iDate + " 数据错误: vol:" + vol + " reserved:" + reserved);

							// TODO:猜测 -- 601988
							vol = 100 * vol;
						}

						// long vol = readLong(value, 24);

						tsData1y.add(new Object[] {iDate, open, high, low, close, amt, vol * 1.00 });

					} // while
					
					logger.info("Year: " + year + " yearDays: " + yearDays);
					if (year >= fromYear && year <= toYear) {
						batchInsertPartition(partition, symbol, tsData1y);
					}

				} catch (FileNotFoundException ex) {
					logger.error(symbol, ex);
				} catch (Exception ex) {
					logger.error(symbol, ex);
				} finally {
					// file close
					try {
						bin.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				long endTime = System.currentTimeMillis();
				logger.info(symbol + "(Time Cost):" + (endTime - startTime) + "ms");
			} 

		}
		
		/**
		 * 按年批次提交
		 * @param partition
		 * @param symbol
		 * @param tsData
		 */
		public void batchInsertPartition(String partition, String symbol, List<Object[]> tsData) {
			
			SqlSession batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
			try {
				TdxStockDayMapper stkDayMapper = batchSqlSession.getMapper(TdxStockDayMapper.class);

				int n = 0;
				int p = 0;
				List<TdxStockDay> records = new ArrayList<TdxStockDay>();
				for (Object[] dayData : tsData) {
					TdxStockDay day = new TdxStockDay();
					day.setSymbol(symbol);
					day.setDate(new SimpleDateFormat("yyyyMMdd").parse(dayData[0].toString()));
					day.setOpen((Double)dayData[1]);
					day.setHigh((Double)dayData[2]);
					day.setLow((Double)dayData[3]);
					day.setClose((Double)dayData[4]);
					day.setAmt((Double)dayData[5]);
					day.setVol((Double)dayData[6]);

					records.add(day);

					n++;

					if (n % 30 == 0) {
						stkDayMapper.batchInsertPartition(partition, records);
						records = new ArrayList<TdxStockDay>();
						p++;
					}

					/*
					if (p % 30 == 0) {
						batchSqlSession.commit();
						batchSqlSession.clearCache();
					}*/
				}

				if (records.size() > 0) {
					stkDayMapper.batchInsertPartition(partition, records);
				}
				
				batchSqlSession.commit();
				batchSqlSession.clearCache();

			} catch (Exception e) {
				// 没有提交的数据可以回滚
				logger.error(symbol, e);
				batchSqlSession.rollback();
			} finally {
				batchSqlSession.close();
			}

		}

	}

	
	public static void decodeTdxIndex(int fromYear, int toYear) {
		ConcurrentLinkedQueue<String> stockQueue = new ConcurrentLinkedQueue<String>();

		stockQueue.addAll(tdxIndex.keySet());

		try {
			Thread[] threads = new Thread[THREADS];
			for (int i = 0; i < THREADS; i++) {
				threads[i] = new Thread(new RobotTask(stockQueue, fromYear, toYear));
				// th.setDaemon(false);
				threads[i].setName("TdxDecoder-TH" + i);
				threads[i].start();
			}

			for (int i = 0; i < THREADS; i++) {
				threads[i].join();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		start(2001, 2005);
		//start(2006, 2010);

		//start(2011,2015);

		//start(2016,2020);
		
		//20171014补充指数数据
		//decodeTdxIndex(2006, 2010);
		//decodeTdxIndex(2011,2015);
		//decodeTdxIndex(2016,2020);
	}

}
