package com.diffwind.dao.mapper;

import java.util.List;
import java.util.Map;

public interface QueryMapper {
   
    List<Map> selectBySuodeshui(List<String> symbols);
    
    //金融行业
    List<Map> selectBySuodeshui_JinRong(List<String> symbols);
    
    //单个查询性能太低
    @Deprecated
	List<Map> selectByShui(String symbol);
    @Deprecated
	List<Map> selectByShui_JinRong(String symbol);
	
    List<Map> selectIncomeStatementBySymbol(String symbol);
    //金融行业
    List<Map> selectIncomeStatementBySymbol_JinRong(String symbol);
   
    //
    List<String> selectRzrqSymbols();
    //融资融券数据
    List<Map> selectRzrqBySymbol(String symbol);
}