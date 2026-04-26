package com.watchlist.controller;

import com.watchlist.common.Result;
import com.watchlist.dto.*;
import com.watchlist.entity.Stock;
import com.watchlist.service.StockService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stocks")
    public Result<List<StockQuoteVO>> listStocks() {
        return Result.success(stockService.listStocksWithQuotes());
    }

    @GetMapping("/stocks/search")
    public Result<List<StockSearchVO>> searchStocks(@RequestParam String keyword) {
        return Result.success(stockService.searchStocks(keyword));
    }

    @PostMapping("/stocks")
    public Result<Stock> addStock(@Valid @RequestBody StockAddRequest request) {
        return Result.success(stockService.addStock(request));
    }

    @PutMapping("/stocks/{id}")
    public Result<Stock> updateStock(@PathVariable Long id,
                                     @Valid @RequestBody StockUpdateRequest request) {
        return Result.success(stockService.updateStock(id, request));
    }

    @DeleteMapping("/stocks/{id}")
    public Result<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return Result.success(null);
    }

    @GetMapping("/quotes")
    public Result<List<StockQuoteVO>> getQuotes(@RequestParam String codes) {
        List<String> codeList = Arrays.asList(codes.split(","));
        return Result.success(stockService.getQuotes(codeList));
    }
}
