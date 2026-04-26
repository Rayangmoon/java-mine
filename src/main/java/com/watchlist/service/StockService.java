package com.watchlist.service;

import com.watchlist.dto.*;
import com.watchlist.entity.Stock;

import java.util.List;

public interface StockService {

    List<StockQuoteVO> listStocksWithQuotes();

    List<StockSearchVO> searchStocks(String keyword);

    Stock addStock(StockAddRequest request);

    Stock updateStock(Long id, StockUpdateRequest request);

    void deleteStock(Long id);

    List<StockQuoteVO> getQuotes(List<String> codes);
}
