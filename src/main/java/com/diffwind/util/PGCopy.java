package com.diffwind.util;

import java.beans.Statement;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import com.diffwind.dao.mapper.TruncateTableMapper;

public class PGCopy {

	private static String OUTPUT_PATH = "xueqiu.day/";

	public static void main(String args[]) {
		Connection c = null;
		PreparedStatement stmt = null;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/diffview", "zhangjz", "");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			/*
			String truncateTableSql = "truncate table xueqiu_stock_day";

			stmt = c.prepareStatement(truncateTableSql);

			stmt.execute();

			String copySql = "copy xueqiu_stock_day from '%s'"
					+ " with (FORMAT csv, DELIMITER ',',header true,quote '\"',encoding 'UTF8')";

			File dir = new File(OUTPUT_PATH);
			File[] dayFiles = dir.listFiles();

			for (int i = 0; i < dayFiles.length; i++) {
				File stockDay = dayFiles[i];

				String filePath = stockDay.getAbsolutePath();

				stmt = c.prepareStatement(String.format(copySql, filePath));

				stmt.execute();

				System.out.println("Finished:" + (i + 1));

			}
			
			
			String createIndexSql = "CREATE INDEX idx1_xueqiu_stock_day ON xueqiu_stock_day (symbol);";
			stmt = c.prepareStatement(createIndexSql);
			stmt.execute();
			*/
			String createIndexSql = "CREATE INDEX idx2_xueqiu_stock_day ON xueqiu_stock_day (date);";
			stmt = c.prepareStatement(createIndexSql);
			stmt.execute();
			

			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Operation done successfully");
	}

}
