package com.akolodziejski.divstock.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TickersService {

    @Autowired
    ResourceLoader resourceLoader;

    public List<String> getTickers() {

        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            resourceLoader.getResource("classpath:/stocks/Tickers-sp500.csv")
                                    .getInputStream()));

            List<String> result = new ArrayList<>();
            String lastLine;
            while((lastLine = br.readLine()) != null) {
                result.add(lastLine);
            }
            return result;
        } catch (IOException e) {
           return Collections.emptyList();
        }
    }
}
