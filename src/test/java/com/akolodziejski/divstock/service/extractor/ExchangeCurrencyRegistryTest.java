package com.akolodziejski.divstock.service.extractor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExchangeCurrencyRegistryTest {

    private final ExchangeCurrencyRegistry registry = new ExchangeCurrencyRegistry();

    @Test
    void nyse_returns_usd() {
        assertThat(registry.getCurrency("NYSE")).isEqualTo("USD");
    }

    @Test
    void nasdaq_returns_usd() {
        assertThat(registry.getCurrency("NASDAQ")).isEqualTo("USD");
    }

    @Test
    void tse_returns_cad() {
        assertThat(registry.getCurrency("TSE")).isEqualTo("CAD");
    }

    @Test
    void lse_returns_gbp() {
        assertThat(registry.getCurrency("LSE")).isEqualTo("GBP");
    }

    @Test
    void sehk_returns_hkd() {
        assertThat(registry.getCurrency("SEHK")).isEqualTo("HKD");
    }

    @Test
    void sgx_returns_sgd() {
        assertThat(registry.getCurrency("SGX")).isEqualTo("SGD");
    }

    @Test
    void aeb_returns_eur() {
        assertThat(registry.getCurrency("AEB")).isEqualTo("EUR");
    }

    @Test
    void unknown_exchange_returns_null() {
        assertThat(registry.getCurrency("UNKNOWN_XYZ")).isNull();
    }
}
