package com.siyan1234.itproject2nd.stock.controller;

import com.siyan1234.itproject2nd.stock.dto.StockDto;
import com.siyan1234.itproject2nd.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/api/stocks")
    public List<StockDto> getMainStocks() {
        return stockService.getMainStocks();
    }
}
