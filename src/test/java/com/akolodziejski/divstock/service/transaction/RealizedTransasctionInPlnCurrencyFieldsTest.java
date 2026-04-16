package com.akolodziejski.divstock.service.transaction;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.NbpPlnRateProvider;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RealizedTransasctionInPlnCurrencyFieldsTest {

    static class FixedRateProvider extends NbpPlnRateProvider {
        FixedRateProvider() { super(null, null); }

        @Override
        public float getRate(String currency, Date date) {
            return 4.0f;
        }
    }

    private final RealizedTransasctionInPln processor =
            new RealizedTransasctionInPln(new FixedRateProvider());

    private Date date(String s) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse(s);
    }

    @Test
    void costInCurrency_is_buy_quantity_times_buy_price() throws Exception {
        Transaction buy = Transaction.builder()
                .symbol("AAPL").currency("USD")
                .date(date("2023-06-01"))
                .quantity(100f).price(10f).proceeds(-1000f).build();
        Transaction sell = Transaction.builder()
                .symbol("AAPL").currency("USD")
                .date(date("2024-03-15"))
                .quantity(-100f).price(15f).proceeds(1500f).build();

        List<PLNTransaction> result = processor.processForYear(List.of(buy, sell), 2024);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCostInCurrency()).isCloseTo(1000f, within(0.01f));
    }

    @Test
    void gainLostInCurrency_is_proceeds_minus_cost_in_original_currency() throws Exception {
        Transaction buy = Transaction.builder()
                .symbol("AAPL").currency("USD")
                .date(date("2023-06-01"))
                .quantity(100f).price(10f).proceeds(-1000f).build();
        Transaction sell = Transaction.builder()
                .symbol("AAPL").currency("USD")
                .date(date("2024-03-15"))
                .quantity(-100f).price(15f).proceeds(1500f).build();

        List<PLNTransaction> result = processor.processForYear(List.of(buy, sell), 2024);

        assertThat(result.get(0).getGainLostInCurrency()).isCloseTo(500f, within(0.01f));
    }

    @Test
    void gainLostInCurrency_is_negative_for_a_loss() throws Exception {
        Transaction buy = Transaction.builder()
                .symbol("MSFT").currency("USD")
                .date(date("2023-06-01"))
                .quantity(100f).price(20f).proceeds(-2000f).build();
        Transaction sell = Transaction.builder()
                .symbol("MSFT").currency("USD")
                .date(date("2024-03-15"))
                .quantity(-100f).price(15f).proceeds(1500f).build();

        List<PLNTransaction> result = processor.processForYear(List.of(buy, sell), 2024);

        assertThat(result.get(0).getGainLostInCurrency()).isCloseTo(-500f, within(0.01f));
    }

    @Test
    void costInCurrency_aggregates_across_multiple_buy_lots() throws Exception {
        Transaction buy1 = Transaction.builder()
                .symbol("GOOG").currency("USD")
                .date(date("2023-01-10"))
                .quantity(50f).price(100f).proceeds(-5000f).build();
        Transaction buy2 = Transaction.builder()
                .symbol("GOOG").currency("USD")
                .date(date("2023-06-01"))
                .quantity(50f).price(120f).proceeds(-6000f).build();
        Transaction sell = Transaction.builder()
                .symbol("GOOG").currency("USD")
                .date(date("2024-03-15"))
                .quantity(-100f).price(150f).proceeds(15000f).build();

        List<PLNTransaction> result = processor.processForYear(List.of(buy1, buy2, sell), 2024);

        assertThat(result.get(0).getCostInCurrency()).isCloseTo(11000f, within(0.01f));
        assertThat(result.get(0).getGainLostInCurrency()).isCloseTo(4000f, within(0.01f));
    }
}
