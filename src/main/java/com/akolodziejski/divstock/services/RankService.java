package com.akolodziejski.divstock.services;

import com.akolodziejski.divstock.model.Rank;
import com.akolodziejski.divstock.services.markers.StockMarker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RankService {

    @Autowired
    private List<StockMarker> markers;

    public Map<String, Double> generateRanks(List<String> tickers) {
        return tickers.stream()
                .map(t -> Map.entry(t ,generateRank(t)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Double generateRank(String ticker) {
        log.info("Generate Rank for {}", ticker);
        return markers.stream().map( it -> it.markStock(ticker)).reduce(0.0,  (f, s) -> f+s);
    }

    @Async
    public void generateAsync(String ticker) {
        log.info("Start generating rank for {} in thread {}", ticker, Thread.currentThread().getName());
        val rank = generateRank(ticker);
    }
}
