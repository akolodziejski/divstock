package com.akolodziejski.divstock.service.tax;

import org.springframework.stereotype.Service;

@Service
public class TaxRateService {

    //TODO: need to unifi and covert ticker into isin
    public double getRate(String isinOrTicker) {

        if(isinOrTicker.startsWith("BTI")) {
            return 0.0;
        }

        if(isinOrTicker.startsWith("GB")) {
            return 0.0;
        }
        return 0.15; // 15%
    }
}
