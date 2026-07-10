package com.siyan1234.itproject2nd.stock.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockApiResponse {
    private StockApiResponseHeaderDto header;
    private StockApiResponseBodyDto body;
}
