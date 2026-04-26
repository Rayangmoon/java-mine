package com.watchlist.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_watchlist")
public class Stock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private String stockName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
