package com.diffwind.stats.filters;

/**
 * 规则库（规则的变更要保留历史版本）：
	//高收益规则1：RANGE(v_netprofit,5) EACHGT 5.0e8 and RANGE(v_roe,5) EACHGT 10
	
	行业规则示例：hangye == '电力行业' and RANGE(v_netprofit,5) EACHGT 5.0e8 and RANGE(v_roe,5) EACHGT 8
	
 * 主要盈利指标: netprofit, roe, roa
 * 主要成长指标: 净资产增长率, 净利润增长率
 * 主要风险指标: pe, pb
 * 主要规模指标: 净资产, 总资产
 * 
 * 行业规则的确定，需要先对行业内股票做分类，以确定规则指标和指标阈值
 * @author billberg
 *
 */
public class FilterRules {

	//盈利指标矩阵：netprofit(10/5/2) x roe (20/15/10)
	//规则执行顺序为按照九宫格宽度优先执行? 
	public static String YINGLI_11 = "(RANGE(v_netprofit,5) EACHGTE 10.0e8) and (RANGE(v_roe,5) EACHGTE 20)";
	public static String YINGLI_12 = "(RANGE(v_netprofit,5) EACHGTE 5.0e8 and RANGE(v_netprofit,5) EACHLT 10.0e8) and (RANGE(v_roe,5) EACHGTE 20)";
	public static String YINGLI_13 = "(RANGE(v_netprofit,5) EACHGT 2.0e8 and RANGE(v_netprofit,5) EACHLT 5.0e8) and (RANGE(v_roe,5) EACHGTE 20)";
	public static String YINGLI_21 = "(RANGE(v_netprofit,5) EACHGTE 10.0e8) and (RANGE(v_roe,5) EACHGTE 15 and RANGE(v_roe,5) EACHLT 20)";
	public static String YINGLI_22 = "(RANGE(v_netprofit,5) EACHGTE 5.0e8 and RANGE(v_netprofit,5) EACHLT 10.0e8) and (RANGE(v_roe,5) EACHGTE 15 and RANGE(v_roe,5) EACHLT 20)";
	public static String YINGLI_23 = "(RANGE(v_netprofit,5) EACHGT 2.0e8 and RANGE(v_netprofit,5) EACHLT 5.0e8) and (RANGE(v_roe,5) EACHGTE 15 and RANGE(v_roe,5) EACHLT 20)";
	public static String YINGLI_31 = "(RANGE(v_netprofit,5) EACHGTE 10.0e8) and (RANGE(v_roe,5) EACHGT 10 and RANGE(v_roe,5) EACHLT 15)";
	public static String YINGLI_32 = "(RANGE(v_netprofit,5) EACHGTE 5.0e8 and RANGE(v_netprofit,5) EACHLT 10.0e8) and (RANGE(v_roe,5) EACHGT 10 and RANGE(v_roe,5) EACHLT 15)";
	public static String YINGLI_33 = "(RANGE(v_netprofit,5) EACHGT 2.0e8 and RANGE(v_netprofit,5) EACHLT 5.0e8) and (RANGE(v_roe,5) EACHGT 10 and RANGE(v_roe,5) EACHLT 15)";
	
	//规则横向表达，按标量方式?不如垂直方式的向量直观
	//foreach(list5Y.stk)(stk.netprofit >= 10.0e8 and stk.roe >= 20)
	//行业规则示例：hangye == '电力行业' and RANGE(v_netprofit,5) EACHGT 5.0e8 and RANGE(v_roe,5) EACHGT 8

	//实际规则: (netprofit > 1.0e9) and (pe > 0 and pe < 11) and (roe > 15 or pb < 1) 
	//筛选时放松pe条件观察优质股的情况
	//TODO: 规则改成最近2年的指标约束，以过滤垃圾企业
	//指标阈值的处理，先检查样本分布，比如确定netprofit的阈值，先通过(pe > 0 and pe < 15) and (roe > 15 or pb < 1)选择样本集合
	public static String LONGWINDCASES_01 = "(netprofit > 5.0e8) and (pe > 0 and pe < 11) and (roe > 15 or pb < 1)";

	@Deprecated
	public static String MINGXING_5Y = "(RANGE(v_netprofit,5) EACHGT 2.0e8) and (RANGE(v_roe,5) EACHGT 15)";
	@Deprecated
	public static String MINGXING_20Q = "(RANGE(v_netprofit4Q,20) EACHGT 2.0e8) and (RANGE(v_roe4Q,20) EACHGT 15)";
	@Deprecated
	public static String MINGXING_10Y = "(RANGE(v_netprofit,10) EACHGT 2.0e8) and (RANGE(v_roe,10) EACHGT 10)";
	
	//同时约束每年和滑动均值
	@Deprecated
	public static String MINGXING_$Y_MA5 = "(RANGE(v_netprofit,{0}) EACHGT 2.0e8) and (RANGE(MA(v_roe,5),{0}) EACHGT 15)";
	
	@Deprecated
	public static String VALUE_$Y_MA5old = "WHICH["
			//明星
			+ "(RANGE(v_netprofit,{0}) EACHGT 2.0e8) and (RANGE(MA(v_roe,5),{0}) EACHGT 15),"
			//高净利润
			+ "(RANGE(v_netprofit,{0}) EACHGT 2.0e8) and (RANGE(v_roe,{0}) EACHGT 10),"
			//高ROE
			+ "(RANGE(v_netprofit,{0}) EACHGT 1.0e8) and (RANGE(MA(v_roe,5),{0}) EACHGT 15),"
			+ "1 >= 0]";
	
	public static String YINGLI_$Y = "WHICH["
			//明星
			+ "(RANGE(v_netprofit,5) EACHGT 2.0e8) and (RANGE(MA(v_roe,3),{0}) EACHGT 15),"
			//高净利润
			+ "(RANGE(v_netprofit,5) EACHGT 5.0e8) and (RANGE(v_roe,{0}) EACHGT 10),"
			//高ROE
			+ "(RANGE(v_netprofit,5) EACHGT 1.0e8) and (RANGE(MA(v_roe,3),{0}) EACHGT 15),"
			//高成长
			+ "(RANGE(v_totalAssets,5) EACHGT 50.0e8) and (RANGE(v_totalNetAssets,5) EACHGT 20.0e8) "
			+ "and (RANGE(v_totalAssetsGrowthRate,5) EACHGT 15) and (RANGE(v_totalNetAssetsGrowthRate,5) EACHGT 15),"
			+ "1 >= 0]";
	
	
	//两步筛选
	//1-所得税 2-盈利能力
	//所得税费用 > 1e8 and 实际所得税率 > 0.1
	//TODO: EBIT, EBITDA
	public static String YINGLI1_SUODESHUI = "(RANGE(v_suodeshui,3) EACHGT 1.0e8) and (RANGE(v_suodeshuiRate,3) EACHGT 0.1)"
			;
	
	//盈利与成长(成长性非常重要-Alpha)
	//成长性只看近3年，有历史数据缺失问题
	//无法处理周期股
	public static String YINGLI2_CLASS_$Y = "WHICH["
			//1-明星(盈利+成长)
			+ "(RANGE(v_netprofit,5) EACHGT 2.0e8) and (RANGE(v_roe,5) EACHGT 15) "
			+ "and ((RANGE(MA(v_totalAssetsGrowthRate,2),5) EACHGT 15) or (RANGE(MA(v_mainBizIncomeGrowthRate,2),5) EACHGT 15)),"
			//2-高成长
			//+ "(RANGE(v_totalAssets,5) EACHGT 50.0e8) and (RANGE(v_totalNetAssets,5) EACHGT 10.0e8) "
			//+ "and (RANGE(v_totalAssetsGrowthRate,5) EACHGT 10) and (RANGE(v_totalNetAssetsGrowthRate,5) EACHGT 10),"
			//+ "(RANGE(v_totalAssets,5) EACHGT 50.0e8) and (RANGE(v_totalNetAssets,5) EACHGT 10.0e8) "
			+ "(RANGE(v_netprofit,5) EACHGT 1.0e8) and (RANGE(v_roe,5) EACHGT 10) "
			+ "and ((RANGE(MA(v_totalAssetsGrowthRate,2),5) EACHGT 15) or (RANGE(MA(v_mainBizIncomeGrowthRate,2),5) EACHGT 15)),"
			//3-高ROE
			+ "(RANGE(v_netprofit,5) EACHGT 1.0e8) and (RANGE(MA(v_roe,2),5) EACHGT 15),"
			//4-高净利润
			+ "(RANGE(v_netprofit,5) EACHGT 5.0e8) and (RANGE(v_roe,5) EACHGT 10),"
			//5-良好
			+ "(RANGE(v_netprofit,5) EACHGT 1.0e8) and (RANGE(v_roe,5) EACHGT 10),"
			//6-近3年关注
			+ "(RANGE(v_netprofit,3) EACHGT 1.0e8) and (RANGE(v_roe,3) EACHGT 15) "
			//+ "and (RANGE(MA(v_totalAssetsGrowthRate,2),3) EACHGT 20),"
			+ "and ((RANGE(MA(v_totalAssetsGrowthRate,2),3) EACHGT 20) or (RANGE(MA(v_mainBizIncomeGrowthRate,2),3) EACHGT 20)),"

			+ "1 >= 0]";

	
	//根据成长性筛选
	//重点考虑总资产增长率、营业收入增长率与营业税？
	public static String CHENGZHANG1 = "";

}
