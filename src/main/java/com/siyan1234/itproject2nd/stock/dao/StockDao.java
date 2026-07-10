package com.siyan1234.itproject2nd.stock.dao;

import com.siyan1234.itproject2nd.stock.dto.StockDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockDao {
    List<StockDto> findMainStocks();
}
