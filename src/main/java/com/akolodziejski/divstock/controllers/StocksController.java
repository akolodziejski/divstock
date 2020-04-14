package com.akolodziejski.divstock.controllers;

import com.akolodziejski.divstock.model.Rank;
import com.akolodziejski.divstock.model.Stock;
import com.akolodziejski.divstock.repositories.RanksRepository;
import com.akolodziejski.divstock.repositories.StocksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
public class StocksController {

    @Autowired
    private StocksRepository stocksRepository;

    @Autowired
    private RanksRepository ranksRepository;

    @GetMapping
    public List<Stock> getAll(){
        return stocksRepository.findAll();
    }

    @GetMapping
    @RequestMapping("{id}")
    public Stock get(@PathVariable Long id) {
        return stocksRepository.getOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Stock create(@RequestBody final Stock stock) {
        return stocksRepository.saveAndFlush(stock);
    }

    @PostMapping
    @RequestMapping("{stock_id}/ranks")
    @ResponseStatus(HttpStatus.CREATED)
    public Stock addRank(@PathVariable Long stock_id, @RequestBody final Rank rank) {

        Stock stock = stocksRepository.getOne(stock_id);
        stock.getRanks().add(rank);
        rank.setStock(stock);
        return stocksRepository.saveAndFlush(stock);
    }
}
