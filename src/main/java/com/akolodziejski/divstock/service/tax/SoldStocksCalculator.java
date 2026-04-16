package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SoldStocksCalculator {

    private static final Logger log = LoggerFactory.getLogger(SoldStocksCalculator.class);

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public SoldStocksCalculator(
            LocalCSVFilesTransactionProvider transactionProvider,
            RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;
        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public List<PLNTransaction> calculate(int year) {
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();
        List<PLNTransaction> all = realizedTransactionCalculator.processForYear(ibTransactions, year);
        all.stream()
                .filter(PLNTransaction::isError)
                .forEach(t -> log.warn("Skipping error transaction for symbol: {}", t.getStatement().getSymbol()));
        return all.stream()
                .filter(t -> !t.isError())
                .collect(Collectors.toList());
    }
}
