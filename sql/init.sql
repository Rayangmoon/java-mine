CREATE DATABASE IF NOT EXISTS stock_watchlist
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE stock_watchlist;

CREATE TABLE IF NOT EXISTS stock_watchlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code  VARCHAR(6)   NOT NULL COMMENT '股票代码，如 600519',
    stock_name  VARCHAR(64)  NOT NULL COMMENT '股票名称',
    notes       VARCHAR(256) DEFAULT '' COMMENT '备注',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
