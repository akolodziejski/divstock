package com.akolodziejski.divstock.service.extractor;

import com.akolodziejski.divstock.model.csv.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Qualifier("ib")
public class InteractiveBrokersCSVExtractor extends CSVExtractor {

    private static final Logger log = LoggerFactory.getLogger(InteractiveBrokersCSVExtractor.class);
    private final ExchangeCurrencyRegistry exchangeCurrencyRegistry = new ExchangeCurrencyRegistry();

    @Override
    protected int getSkipLines() {
        return 7;
    }

    @Override
    protected List<Transaction> mapToTransactions(List<String[]> lines) {
        Map<String, String> symbolCurrencyToExchange = buildExchangeMap(lines);

        return lines.stream()
                .filter(row -> row[2].equals("Order"))
                .filter(row -> row[3].equals("Stocks"))
                .map(row -> mapToTransaction(row, symbolCurrencyToExchange))
                .collect(Collectors.toList());
    }

    private Map<String, String> buildExchangeMap(List<String[]> lines) {
        Map<String, String> map = new HashMap<>();
        for (String[] row : lines) {
            if (row.length > 8
                    && "Financial Instrument Information".equals(row[0])
                    && "Data".equals(row[1])
                    && "Stocks".equals(row[2])) {
                String symbol = row[3];
                String exchange = row[8];
                String currency = exchangeCurrencyRegistry.getCurrency(exchange);
                if (currency == null) {
                    log.warn("No currency mapping for exchange '{}' (symbol: {})", exchange, symbol);
                    continue;
                }
                map.put(symbol + "/" + currency, exchange);
            }
        }
        return map;
    }

    protected Transaction mapToTransaction(String[] row, Map<String, String> symbolCurrencyToExchange) {
        String currency = row[4];
        String symbol = row[5];
        String listingExch = symbolCurrencyToExchange.get(symbol + "/" + currency);
        if (listingExch == null) {
            log.warn("No listing exchange found for symbol '{}' currency '{}'", symbol, currency);
        }
        return Transaction.builder()
                .currency(currency)
                .symbol(symbol)
                .date(parseDate(row[6]))
                .quantity(Float.valueOf(row[7]))
                .price(Float.valueOf(row[8]))
                .proceeds(Float.valueOf(row[9]))
                .fee(Float.valueOf(row[10]))
                .listingExch(listingExch)
                .build();
    }

    @Override
    protected Transaction mapToTransaction(String[] row) {
        return mapToTransaction(row, Map.of());
    }

    private Date parseDate(String dateTime) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            String datePart = dateTime.split(",")[0];
            if (datePart == null || datePart.isBlank()) return null;
            return formatter.parse(datePart);
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateTime);
            return null;
        }
    }
}
