package com.akolodziejski.divstock.service;

import com.akolodziejski.divstock.model.api.NBPCurrencyRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class NbpPlnRateProvider {

    private static String NBP_API_RATES = "https://api.nbp.pl/api/exchangerates/rates/a/";
    private static SimpleDateFormat NBP_API_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    private final RestTemplate restTemplate;

    public NbpPlnRateProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public float getRate(String currency, Date date) {
        return restTemplate.getForObject(getApiUrl(currency, date), NBPCurrencyRate.class).getRates()
                    .stream().findFirst().get().getMid();
    }

    private static String getApiUrl(String currency, Date date) {
        String dateAsString = NBP_API_DATE_FORMATTER.format(date);
        return NBP_API_RATES + currency + "/" + dateAsString + "?format=json";
    }
}
