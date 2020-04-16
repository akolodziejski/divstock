package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.Stock;
import com.akolodziejski.divstock.repositories.StocksRepository;
import com.akolodziejski.divstock.services.TickersService;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

public class StartupActions {
}

@Slf4j
@Component
class StockLoader {

    @Autowired
    private StocksRepository stocksRepository;

    @Autowired
    private TickersService tickersService;

    @EventListener(ApplicationReadyEvent.class)
    void onReadyApplication() {
        log.info("Loading Stocks into DB!");

        var stocks = tickersService.getTickers().stream()
                .map(ticker -> createStock(ticker))
                .collect(toList());
        stocksRepository.saveAll(stocks);
        stocksRepository.flush();
        log.info("Loaded {} stocks into DB ", stocks.size());
    }

    private Stock createStock(String ticker) {
        var stock = new Stock();
        stock.setTicker(ticker);
        return stock;
    }
}