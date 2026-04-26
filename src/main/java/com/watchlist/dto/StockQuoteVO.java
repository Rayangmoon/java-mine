package com.watchlist.dto;

import lombok.Data;

@Data
public class StockQuoteVO {
    private Long id;
    private String stockCode;
    private String stockName;
    private Double currentPrice;
    private Double todayOpen;
    private Double yesterdayClose;
    private Double high;
    private Double low;
    private Long volume;
    private String notes;
}
