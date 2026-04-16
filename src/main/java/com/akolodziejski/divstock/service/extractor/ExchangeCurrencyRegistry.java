package com.akolodziejski.divstock.service.extractor;

import java.util.Map;

public class ExchangeCurrencyRegistry {

    private static final Map<String, String> EXCHANGE_TO_CURRENCY = Map.of(
            "NYSE",   "USD",
            "NASDAQ", "USD",
            "AMEX",   "USD",
            "TSE",    "CAD",
            "LSE",    "GBP",
            "SEHK",   "HKD",
            "SGX",    "SGD",
            "AEB",    "EUR"
    );

    public String getCurrency(String exchange) {
        return EXCHANGE_TO_CURRENCY.get(exchange);
    }
}
