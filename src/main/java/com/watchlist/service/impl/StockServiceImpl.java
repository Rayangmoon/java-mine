package com.watchlist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.watchlist.common.BusinessException;
import com.watchlist.dto.*;
import com.watchlist.entity.Stock;
import com.watchlist.mapper.StockMapper;
import com.watchlist.service.SinaStockApi;
import com.watchlist.service.StockService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final SinaStockApi sinaStockApi;

    public StockServiceImpl(StockMapper stockMapper, SinaStockApi sinaStockApi) {
        this.stockMapper = stockMapper;
        this.sinaStockApi = sinaStockApi;
    }

    @Override
    public List<StockQuoteVO> listStocksWithQuotes() {
        List<Stock> stocks = stockMapper.selectList(null);
        if (stocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> codes = stocks.stream()
                .map(Stock::getStockCode)
                .collect(Collectors.toList());

        List<StockQuoteVO> quotes;
        try {
            quotes = sinaStockApi.getQuotes(codes);
        } catch (Exception e) {
            quotes = Collections.emptyList();
        }

        Map<String, StockQuoteVO> quoteMap = quotes.stream()
                .collect(Collectors.toMap(StockQuoteVO::getStockCode, v -> v, (a, b) -> a));

        return stocks.stream().map(stock -> {
            StockQuoteVO vo = quoteMap.getOrDefault(
                    stock.getStockCode(), new StockQuoteVO());
            vo.setId(stock.getId());
            vo.setStockCode(stock.getStockCode());
            vo.setStockName(stock.getStockName());
            vo.setNotes(stock.getNotes());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<StockSearchVO> searchStocks(String keyword) {
        if (keyword.matches("^[03468]\\d{5}$")) {
            try {
                List<StockQuoteVO> quotes = sinaStockApi.getQuotes(
                        Collections.singletonList(keyword));
                return quotes.stream().map(q -> {
                    StockSearchVO vo = new StockSearchVO();
                    vo.setStockCode(q.getStockCode());
                    vo.setStockName(q.getStockName());
                    return vo;
                }).collect(Collectors.toList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return sinaStockApi.search(keyword);
    }

    @Override
    public Stock addStock(StockAddRequest request) {
        String stockCode = request.getStockCode();

        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_code", stockCode);
        if (stockMapper.selectOne(wrapper) != null) {
            throw new BusinessException(409, "该股票已在自选列表中");
        }

        List<StockQuoteVO> quotes = sinaStockApi.getQuotes(
                Collections.singletonList(stockCode));
        if (quotes.isEmpty()) {
            throw new BusinessException(400, "无效的股票代码");
        }

        Stock stock = new Stock();
        stock.setStockCode(stockCode);
        stock.setStockName(quotes.get(0).getStockName());
        stock.setNotes(request.getNotes() != null ? request.getNotes() : "");
        stockMapper.insert(stock);

        return stock;
    }

    @Override
    public Stock updateStock(Long id, StockUpdateRequest request) {
        Stock stock = stockMapper.selectById(id);
        if (stock == null) {
            throw new BusinessException(404, "自选股不存在");
        }
        stock.setNotes(request.getNotes());
        stockMapper.updateById(stock);
        return stock;
    }

    @Override
    public void deleteStock(Long id) {
        if (stockMapper.selectById(id) == null) {
            throw new BusinessException(404, "自选股不存在");
        }
        stockMapper.deleteById(id);
    }

    @Override
    public List<StockQuoteVO> getQuotes(List<String> codes) {
        return sinaStockApi.getQuotes(codes);
    }
}
