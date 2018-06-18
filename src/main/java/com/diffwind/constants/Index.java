package com.diffwind.constants;

import java.util.HashMap;

public class Index {
	// 通达信指数代码
	public static final String[] indexSymbols = {"sh000300","sh999999","sz399001","sz399005","sz399006"};
	public static final HashMap<String, String> tdxIndex = new HashMap<String, String>() {
		{
			put("sh999999", "上证指数");
			put("sz399001", "深证成指");
			put("sz399005", "中小板指");
			put("sz399006", "创业板指");
			put("sh000300", "沪深300");
		}
	};

}
