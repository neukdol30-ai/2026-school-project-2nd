package com.siyan1234.itproject2nd.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {
    private int id;
    private String symbol;
    private String name;
    private long price;
    private long changePrice;
    private double changeRate;
    private List<StockPointDto> points;
}
