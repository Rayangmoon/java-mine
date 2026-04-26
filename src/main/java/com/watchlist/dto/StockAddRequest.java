package com.watchlist.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class StockAddRequest {

    @NotBlank(message = "股票代码不能为空")
    @Pattern(regexp = "^[03468]\\d{5}$", message = "股票代码格式不正确，需为6位数字且首位为0/3/4/6/8")
    private String stockCode;

    private String notes;
}
