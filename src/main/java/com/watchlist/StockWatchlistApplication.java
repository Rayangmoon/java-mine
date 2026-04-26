package com.watchlist;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.watchlist.mapper")
public class StockWatchlistApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockWatchlistApplication.class, args);
    }
}
