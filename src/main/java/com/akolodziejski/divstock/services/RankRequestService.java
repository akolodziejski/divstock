package com.akolodziejski.divstock.services;

import com.akolodziejski.divstock.RabbitMqConfig;
import com.akolodziejski.divstock.model.RankRequest;
import com.akolodziejski.divstock.repositories.StocksRepository;
import lombok.val;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankRequestService {

    private final String ALL_MATCH = "*";
    private final String RANK_GENERATOR_ROUTING_KEY = "rank.generator.start";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StocksRepository stocksRepository;

    public void queueRequest(RankRequest request) {

        val ticker = request.getTicker();
        if (ticker.equals(ALL_MATCH)) {
             sendForAllStocks();
             return;
        }
        sendForOneStock(ticker);
    }

    private void sendForOneStock(String ticker) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.rankExchange, RANK_GENERATOR_ROUTING_KEY, ticker);
    }

    private void sendForAllStocks() {
        stocksRepository.findAll().forEach( stock -> sendForOneStock(stock.getTicker()));
    }
}
