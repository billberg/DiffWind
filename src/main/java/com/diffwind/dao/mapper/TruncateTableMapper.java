package com.diffwind.dao.mapper;

import org.apache.ibatis.annotations.Param;

public interface TruncateTableMapper {
    
    //void createIndex();    
    
   // void dropIndex();
    
    void truncateTable(@Param("tableName") String tableName);
    
    void swapTable(@Param("tableName") String tableName);
}