package com.watchlist.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class StockUpdateRequest {

    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String notes;
}
