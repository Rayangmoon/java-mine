package com.watchlist.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.watchlist.entity.Stock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockMapperTest {

    @Autowired
    private StockMapper stockMapper;

    @Test
    void testInsertAndSelectById() {
        Stock stock = new Stock();
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");
        stock.setNotes("长期持有");

        int rows = stockMapper.insert(stock);
        assertEquals(1, rows);
        assertNotNull(stock.getId());

        Stock found = stockMapper.selectById(stock.getId());
        assertEquals("600519", found.getStockCode());
        assertEquals("贵州茅台", found.getStockName());
    }

    @Test
    void testSelectByStockCode() {
        Stock stock = new Stock();
        stock.setStockCode("000001");
        stock.setStockName("平安银行");
        stock.setNotes("");
        stockMapper.insert(stock);

        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_code", "000001");
        Stock found = stockMapper.selectOne(wrapper);

        assertNotNull(found);
        assertEquals("平安银行", found.getStockName());
    }

    @Test
    void testDeleteById() {
        Stock stock = new Stock();
        stock.setStockCode("300750");
        stock.setStockName("宁德时代");
        stockMapper.insert(stock);

        int rows = stockMapper.deleteById(stock.getId());
        assertEquals(1, rows);
        assertNull(stockMapper.selectById(stock.getId()));
    }
}
