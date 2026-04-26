package com.watchlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchlist.dto.StockAddRequest;
import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockSearchVO;
import com.watchlist.entity.Stock;
import com.watchlist.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listStocks_returnsQuotes() throws Exception {
        StockQuoteVO vo = new StockQuoteVO();
        vo.setId(1L);
        vo.setStockCode("600519");
        vo.setStockName("贵州茅台");
        vo.setCurrentPrice(1688.0);

        when(stockService.listStocksWithQuotes())
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
    }

    @Test
    void searchStocks_returnsResults() throws Exception {
        StockSearchVO vo = new StockSearchVO();
        vo.setStockCode("600519");
        vo.setStockName("贵州茅台");

        when(stockService.searchStocks("茅台"))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/stocks/search").param("keyword", "茅台"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockName").value("贵州茅台"));
    }

    @Test
    void addStock_validRequest_returnsStock() throws Exception {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");

        when(stockService.addStock(any(StockAddRequest.class))).thenReturn(stock);

        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        mockMvc.perform(post("/api/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockCode").value("600519"));
    }

    @Test
    void addStock_invalidCode_returns400() throws Exception {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("12345");

        mockMvc.perform(post("/api/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void updateStock_returnsUpdatedStock() throws Exception {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");
        stock.setNotes("核心持仓");

        when(stockService.updateStock(any(Long.class), any())).thenReturn(stock);

        mockMvc.perform(put("/api/stocks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"核心持仓\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("核心持仓"));
    }

    @Test
    void deleteStock_returns200() throws Exception {
        mockMvc.perform(delete("/api/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getQuotes_returnsQuotes() throws Exception {
        StockQuoteVO vo = new StockQuoteVO();
        vo.setStockCode("600519");
        vo.setStockName("贵州茅台");

        when(stockService.getQuotes(Arrays.asList("600519", "000001")))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/quotes").param("codes", "600519,000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
    }
}
