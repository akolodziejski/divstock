package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ExchangeIncomeCalculatorTest {

    static class TestTransactionProvider extends LocalCSVFilesTransactionProvider {
        private final List<Transaction> testTransactions;

        TestTransactionProvider(List<Transaction> testTransactions) {
            super(null, null, null, null, null, null);
            this.testTransactions = testTransactions;
        }

        @Override
        public List<Transaction> getIbTransactions() {
            return testTransactions;
        }
    }

    static class TestRealizedTransactionCalculator extends RealizedTransasctionInPln {
        private final List<PLNTransaction> testTransactions;

        TestRealizedTransactionCalculator(List<PLNTransaction> testTransactions) {
            super(null);
            this.testTransactions = testTransactions;
        }

        @Override
        public List<PLNTransaction> processForYear(List<Transaction> ibTransactions, int year) {
            return testTransactions;
        }
    }

    private ExchangeIncomeCalculator calculator;

    private void setUp(List<PLNTransaction> plnTransactions) {
        var transactionProvider = new TestTransactionProvider(List.of());
        var realizedTransactionCalculator = new TestRealizedTransactionCalculator(plnTransactions);
        calculator = new ExchangeIncomeCalculator(transactionProvider, realizedTransactionCalculator);
    }

    private PLNTransaction plnTx(String exchange, float income, float cost) {
        Transaction statement = Transaction.builder()
                .symbol("AAPL").currency("USD").listingExch(exchange).build();
        return PLNTransaction.builder()
                .income(income).cost(cost).gainLost(0).error(false).statement(statement).build();
    }

    private PLNTransaction errorTx(String exchange) {
        Transaction statement = Transaction.builder()
                .symbol("ERR").currency("USD").listingExch(exchange).build();
        return PLNTransaction.builder()
                .income(999).cost(500).gainLost(0).error(true).statement(statement).build();
    }

    @Test
    void groups_income_and_cost_by_exchange() {
        setUp(List.of(
                plnTx("NYSE", 1000f, 800f),
                plnTx("NYSE", 500f, 400f),
                plnTx("TSE", 200f, 150f)
        ));

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result.get("NYSE").income()).isCloseTo(1500.0, within(0.01));
        assertThat(result.get("NYSE").cost()).isCloseTo(1200.0, within(0.01));
        assertThat(result.get("TSE").income()).isCloseTo(200.0, within(0.01));
        assertThat(result.get("TSE").cost()).isCloseTo(150.0, within(0.01));
    }

    @Test
    void error_transactions_are_excluded() {
        setUp(List.of(plnTx("NYSE", 1000f, 800f), errorTx("NYSE")));

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result.get("NYSE").income()).isCloseTo(1000.0, within(0.01));
        assertThat(result.get("NYSE").cost()).isCloseTo(800.0, within(0.01));
    }

    @Test
    void null_listingExch_grouped_under_unknown() {
        Transaction statement = Transaction.builder()
                .symbol("XYZ").currency("USD").listingExch(null).build();
        PLNTransaction nullExchange = PLNTransaction.builder()
                .income(300f).cost(250f).gainLost(0).error(false).statement(statement).build();

        setUp(List.of(nullExchange));

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result.get("UNKNOWN").income()).isCloseTo(300.0, within(0.01));
        assertThat(result.get("UNKNOWN").cost()).isCloseTo(250.0, within(0.01));
    }

    @Test
    void empty_transactions_returns_empty_map() {
        setUp(List.of());

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result).isEmpty();
    }
}
