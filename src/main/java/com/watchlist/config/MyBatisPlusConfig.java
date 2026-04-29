package com.watchlist.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.watchlist.mapper")
public class MyBatisPlusConfig {
}
