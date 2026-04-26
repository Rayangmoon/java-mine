package com.watchlist.service;

import com.watchlist.common.BusinessException;
import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockSearchVO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SinaStockApi {

    private final RestTemplate restTemplate;

    public SinaStockApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static String getExchangePrefix(String stockCode) {
        char first = stockCode.charAt(0);
        switch (first) {
            case '6': return "sh";
            case '0': case '3': return "sz";
            case '4': case '8': return "bj";
            default:
                throw new BusinessException(400, "无法识别的股票代码前缀: " + stockCode);
        }
    }

    public List<StockQuoteVO> getQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        String codeList = stockCodes.stream()
                .map(code -> getExchangePrefix(code) + code)
                .collect(Collectors.joining(","));

        String url = "https://hq.sinajs.cn/list=" + codeList;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://finance.sina.com.cn");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);

        byte[] bytes = response.getBody();
        if (bytes == null) return Collections.emptyList();

        String body = new String(bytes, Charset.forName("GBK"));
        return parseQuoteResponse(body);
    }

    public List<StockSearchVO> search(String keyword) {
        String encodedKeyword;
        try {
            encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Collections.emptyList();
        }

        String url = "https://suggest3.sinajs.cn/suggest/type=11&key=" + encodedKeyword;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://finance.sina.com.cn");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);

        byte[] bytes = response.getBody();
        if (bytes == null) return Collections.emptyList();

        String body = new String(bytes, Charset.forName("GBK"));
        return parseSearchResponse(body);
    }

    List<StockQuoteVO> parseQuoteResponse(String body) {
        List<StockQuoteVO> result = new ArrayList<>();
        String[] lines = body.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || !line.contains("\"")) continue;

            String fullCode = line.substring(line.indexOf("str_") + 4, line.indexOf("="));
            String stockCode = fullCode.substring(2);

            String data = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
            if (data.isEmpty()) continue;

            String[] fields = data.split(",");
            if (fields.length < 9) continue;

            StockQuoteVO vo = new StockQuoteVO();
            vo.setStockCode(stockCode);
            vo.setStockName(fields[0]);
            vo.setTodayOpen(parseDouble(fields[1]));
            vo.setYesterdayClose(parseDouble(fields[2]));
            vo.setCurrentPrice(parseDouble(fields[3]));
            vo.setHigh(parseDouble(fields[4]));
            vo.setLow(parseDouble(fields[5]));
            vo.setVolume(parseLong(fields[8]));
            result.add(vo);
        }
        return result;
    }

    List<StockSearchVO> parseSearchResponse(String body) {
        List<StockSearchVO> result = new ArrayList<>();

        int start = body.indexOf("\"");
        int end = body.lastIndexOf("\"");
        if (start < 0 || end <= start) return result;

        String data = body.substring(start + 1, end);
        if (data.isEmpty()) return result;

        String[] entries = data.split(";");
        for (String entry : entries) {
            String[] fields = entry.split(",");
            if (fields.length < 4) continue;

            StockSearchVO vo = new StockSearchVO();
            String fullCode = fields[1];
            vo.setStockCode(fullCode.replaceAll("^(sh|sz|bj)", ""));
            vo.setStockName(fields[2]);
            result.add(vo);
        }
        return result;
    }

    private Double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }

    private Long parseLong(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return null; }
    }
}
