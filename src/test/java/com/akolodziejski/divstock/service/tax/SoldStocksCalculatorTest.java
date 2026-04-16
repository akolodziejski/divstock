package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SoldStocksCalculatorTest {

    static class TestTransactionProvider extends LocalCSVFilesTransactionProvider {
        private final List<Transaction> transactions;
        TestTransactionProvider(List<Transaction> transactions) {
            super(null, null, null, null, null, null);
            this.transactions = transactions;
        }
        @Override
        public List<Transaction> getIbTransactions() { return transactions; }
    }

    static class TestRealizedCalculator extends RealizedTransasctionInPln {
        private final List<PLNTransaction> result;
        TestRealizedCalculator(List<PLNTransaction> result) { super(null); this.result = result; }
        @Override
        public List<PLNTransaction> processForYear(List<Transaction> txs, int year) { return result; }
    }

    private SoldStocksCalculator buildCalculator(List<PLNTransaction> plnTransactions) {
        return new SoldStocksCalculator(
                new TestTransactionProvider(List.of()),
                new TestRealizedCalculator(plnTransactions));
    }

    private PLNTransaction okTx(String symbol) {
        return PLNTransaction.builder()
                .error(false)
                .gainLost(100f).cost(80f).income(180f)
                .costInCurrency(20f).gainLostInCurrency(25f)
                .statement(Transaction.builder()
                        .symbol(symbol).currency("USD").quantity(-10f)
                        .price(18f).proceeds(180f).build())
                .build();
    }

    private PLNTransaction errorTx(String symbol) {
        return PLNTransaction.builder()
                .error(true)
                .statement(Transaction.builder().symbol(symbol).currency("USD").build())
                .build();
    }

    @Test
    void returns_non_error_transactions_for_year() {
        List<PLNTransaction> result = buildCalculator(List.of(okTx("AAPL"), okTx("GOOG")))
                .calculate(2024);

        assertThat(result).hasSize(2);
    }

    @Test
    void error_transactions_are_excluded() {
        List<PLNTransaction> result = buildCalculator(List.of(okTx("AAPL"), errorTx("ERR")))
                .calculate(2024);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatement().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void empty_input_returns_empty_list() {
        List<PLNTransaction> result = buildCalculator(List.of()).calculate(2024);

        assertThat(result).isEmpty();
    }
}
