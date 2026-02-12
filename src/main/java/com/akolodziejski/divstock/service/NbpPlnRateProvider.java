package com.akolodziejski.divstock.service;

import com.akolodziejski.divstock.model.api.NBPCurrencyRate;

import com.akolodziejski.divstock.cache.StockPriceCache;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.Date;

@Service
public class NbpPlnRateProvider {

    private static String NBP_API_RATES = "https://api.nbp.pl/api/exchangerates/rates/a/";

    private final RestTemplate restTemplate;
    private final StockPriceCache stockPriceCache;

    public NbpPlnRateProvider(RestTemplate restTemplate, StockPriceCache stockPriceCache) {
        this.restTemplate = restTemplate;
        this.stockPriceCache = stockPriceCache;
    }

    public float getRate(String currency, Date date) {

        Float cachedPrice = stockPriceCache.get(currency, date);
        if(cachedPrice != null) {
            return cachedPrice;
        }

        // GBX => GBP case
        boolean isGBXCase = false;
        if ("GBX".equalsIgnoreCase(currency)) {
            currency = "GBP";
            isGBXCase = true;
        }

        float price = getForNowOrPrevWorkingDay(currency, date);

        if (isGBXCase) {
            price = price / 100;
        }

        stockPriceCache.put(currency, date, price);

        return price;
    }

    private float getForNowOrPrevWorkingDay(String currency, Date date) {
        try {
            return restTemplate.getForObject(getApiUrl(currency, date), NBPCurrencyRate.class)
                    .getRates().stream().findFirst().get().getMid();
        }catch (HttpClientErrorException ex) {
            //TODO ugly
            return getForNowOrPrevWorkingDay(currency, Common.getPreviousWorkingDay(date));
        }
    }

    private static String getApiUrl(String currency, Date date) {
        String dateAsString = Common.SIMPLE_FORMAT.format(date);
        return NBP_API_RATES + currency + "/" + dateAsString + "?format=json";
    }
}
