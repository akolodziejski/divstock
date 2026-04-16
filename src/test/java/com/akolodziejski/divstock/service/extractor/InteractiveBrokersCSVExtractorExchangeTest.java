package com.akolodziejski.divstock.service.extractor;

import com.akolodziejski.divstock.model.csv.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InteractiveBrokersCSVExtractorExchangeTest {

    private final InteractiveBrokersCSVExtractor extractor = new InteractiveBrokersCSVExtractor();

    private String[] tradesRow(String currency, String symbol, String quantity) {
        return new String[]{"Trades", "Data", "Order", "Stocks", currency, symbol,
                "2024-01-15", quantity, "100.0", "10000.0", "-1.5"};
    }

    private String[] fiiRow(String symbol, String listingExch) {
        return new String[]{"Financial Instrument Information", "Data", "Stocks", symbol,
                "Some Description", "12345", "US0000000000", symbol, listingExch, "1", "COMMON", ""};
    }

    @Test
    void listingExch_is_populated_for_simple_case() {
        List<String[]> lines = List.of(
                fiiRow("AAPL", "NASDAQ"),
                tradesRow("USD", "AAPL", "-10")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("NASDAQ");
    }

    @Test
    void cross_listed_stock_resolved_by_currency_usd_to_nyse() {
        List<String[]> lines = List.of(
                fiiRow("ENB", "NYSE"),
                fiiRow("ENB", "TSE"),
                tradesRow("USD", "ENB", "-5")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("NYSE");
    }

    @Test
    void cross_listed_stock_resolved_by_currency_cad_to_tse() {
        List<String[]> lines = List.of(
                fiiRow("ENB", "NYSE"),
                fiiRow("ENB", "TSE"),
                tradesRow("CAD", "ENB", "-5")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("TSE");
    }

    @Test
    void buy_transactions_have_listingExch_set() {
        List<String[]> lines = List.of(
                fiiRow("AAPL", "NASDAQ"),
                tradesRow("USD", "AAPL", "10")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("NASDAQ");
    }

    @Test
    void symbol_without_fii_row_has_null_listingExch() {
        List<String[]> lines = List.<String[]>of(
                tradesRow("USD", "XYZ", "-3")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isNull();
    }
}
