package com.akolodziejski.divstock.controllers;

import com.akolodziejski.divstock.model.Rank;
import com.akolodziejski.divstock.model.RankRequest;
import com.akolodziejski.divstock.model.Stock;
import com.akolodziejski.divstock.repositories.RanksRepository;
import com.akolodziejski.divstock.repositories.StocksRepository;
import com.akolodziejski.divstock.services.RankRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranks")
public class RanksController {

    @Autowired
    RankRequestService rankRequestService;

    @Autowired
    private RanksRepository ranksRepository;

    @GetMapping
    public List<Rank> getAll(){
        return ranksRepository.findAll();
    }

    @GetMapping
    @RequestMapping("{id}")
    public Rank get(@PathVariable Long id) {
        return ranksRepository.getOne(id);
    }

    @PostMapping()
    @RequestMapping("/requests")
    public void requestForRank(@RequestBody final RankRequest rankRequest) {
        rankRequestService.queueRequest(rankRequest);
    }
}
