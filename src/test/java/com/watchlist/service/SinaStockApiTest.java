package com.watchlist.service;

import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockSearchVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SinaStockApiTest {

    private final SinaStockApi api = new SinaStockApi(null);

    @Test
    void parseQuoteResponse_normal() {
        String body = "var hq_str_sh600519=\"贵州茅台,1688.00,1682.00,1690.00,1695.00,1680.00,1689.00,1690.00,12345678,20000000000.00,\";\n";

        List<StockQuoteVO> result = api.parseQuoteResponse(body);

        assertEquals(1, result.size());
        assertEquals("600519", result.get(0).getStockCode());
        assertEquals("贵州茅台", result.get(0).getStockName());
        assertEquals(1690.00, result.get(0).getCurrentPrice());
        assertEquals(12345678L, result.get(0).getVolume());
    }

    @Test
    void parseQuoteResponse_emptyData() {
        String body = "var hq_str_sh999999=\"\";\n";

        List<StockQuoteVO> result = api.parseQuoteResponse(body);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseSearchResponse_normal() {
        String body = "var suggestvalue=\"11,sh600519,贵州茅台,贵州茅台,gzmt;11,sz000858,五粮液,五粮液,wly\";";

        List<StockSearchVO> result = api.parseSearchResponse(body);

        assertEquals(2, result.size());
        assertEquals("600519", result.get(0).getStockCode());
        assertEquals("贵州茅台", result.get(0).getStockName());
        assertEquals("000858", result.get(1).getStockCode());
    }

    @Test
    void parseSearchResponse_empty() {
        String body = "var suggestvalue=\"\";";

        List<StockSearchVO> result = api.parseSearchResponse(body);
        assertTrue(result.isEmpty());
    }

    @Test
    void getExchangePrefix_allCases() {
        assertEquals("sh", SinaStockApi.getExchangePrefix("600519"));
        assertEquals("sz", SinaStockApi.getExchangePrefix("000001"));
        assertEquals("sz", SinaStockApi.getExchangePrefix("300750"));
        assertEquals("bj", SinaStockApi.getExchangePrefix("430047"));
        assertEquals("bj", SinaStockApi.getExchangePrefix("830799"));
    }
}
