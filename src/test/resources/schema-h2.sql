DROP TABLE IF EXISTS stock_watchlist;

CREATE TABLE stock_watchlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code  VARCHAR(6)   NOT NULL,
    stock_name  VARCHAR(64)  NOT NULL,
    notes       VARCHAR(256) DEFAULT '',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_stock_code ON stock_watchlist(stock_code);
