package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExchangeIncomeCalculator {

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public ExchangeIncomeCalculator(
            LocalCSVFilesTransactionProvider transactionProvider,
            RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;
        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public Map<String, Double> calculate(int year) {
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();
        List<PLNTransaction> plnTransactions = realizedTransactionCalculator.processForYear(ibTransactions, year);

        return plnTransactions.stream()
                .filter(t -> !t.isError())
                .collect(Collectors.groupingBy(
                        t -> {
                            String exch = t.getStatement().getListingExch();
                            return exch != null ? exch : "UNKNOWN";
                        },
                        Collectors.summingDouble(t -> t.getIncome())
                ));
    }
}
