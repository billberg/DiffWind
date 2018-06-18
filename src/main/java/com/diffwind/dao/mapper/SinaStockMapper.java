package com.diffwind.dao.mapper;

import java.util.List;

import com.diffwind.dao.model.SinaStock;

public interface SinaStockMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sina_stock
     *
     * @mbggenerated
     */
    int deleteByPrimaryKey(String symbol);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sina_stock
     *
     * @mbggenerated
     */
    int insert(SinaStock record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sina_stock
     *
     * @mbggenerated
     */
    SinaStock selectByPrimaryKey(String symbol);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sina_stock
     *
     * @mbggenerated
     */
    List<SinaStock> selectAll();

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sina_stock
     *
     * @mbggenerated
     */
    int updateByPrimaryKey(SinaStock record);
}