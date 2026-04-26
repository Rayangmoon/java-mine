package com.watchlist.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.watchlist.common.BusinessException;
import com.watchlist.dto.StockAddRequest;
import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockUpdateRequest;
import com.watchlist.entity.Stock;
import com.watchlist.mapper.StockMapper;
import com.watchlist.service.impl.StockServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private SinaStockApi sinaStockApi;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    void addStock_success() {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        when(stockMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        StockQuoteVO quoteVO = new StockQuoteVO();
        quoteVO.setStockCode("600519");
        quoteVO.setStockName("贵州茅台");
        when(sinaStockApi.getQuotes(anyList()))
                .thenReturn(Collections.singletonList(quoteVO));

        when(stockMapper.insert(any(Stock.class))).thenReturn(1);

        Stock result = stockService.addStock(request);

        assertEquals("600519", result.getStockCode());
        assertEquals("贵州茅台", result.getStockName());
        verify(stockMapper).insert(any(Stock.class));
    }

    @Test
    void addStock_duplicate_throwsException() {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        Stock existing = new Stock();
        existing.setStockCode("600519");
        when(stockMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.addStock(request));
        assertEquals(409, ex.getCode());
    }

    @Test
    void addStock_invalidCode_throwsException() {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        when(stockMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(sinaStockApi.getQuotes(anyList())).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.addStock(request));
        assertEquals(400, ex.getCode());
    }

    @Test
    void updateStock_notFound_throwsException() {
        when(stockMapper.selectById(999L)).thenReturn(null);

        StockUpdateRequest request = new StockUpdateRequest();
        request.setNotes("新备注");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.updateStock(999L, request));
        assertEquals(404, ex.getCode());
    }

    @Test
    void deleteStock_notFound_throwsException() {
        when(stockMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.deleteStock(999L));
        assertEquals(404, ex.getCode());
    }
}
