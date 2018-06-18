package com.diffwind.dao.mapper;

import org.apache.ibatis.annotations.Param;

public interface CreateIndexMapper {
    
  
    void createIndex(@Param("sql") String sql);
    
    void dropIndex(@Param("idx") String idx);
    
}