package com.akolodziejski.divstock.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/app")
public class ApplicationController {

    @Autowired
    private CacheManager  cacheManager;


    @PostMapping()
    @RequestMapping("/cache/clearRequest")
    public void requestForRank() {
        final var cacheName = "rank";
        cacheManager.getCache(cacheName).clear();
        log.info("Cache {} cleared.", cacheName);
    }
}
