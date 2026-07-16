package com.siyan1234.itproject2nd.stock.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StockApiItemsDto {
    private List<StockApiItemDto> item;
}
