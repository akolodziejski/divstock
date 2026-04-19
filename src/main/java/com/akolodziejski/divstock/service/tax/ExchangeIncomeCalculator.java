package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExchangeIncomeCalculator {

    private static final Logger log = LoggerFactory.getLogger(ExchangeIncomeCalculator.class);

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public ExchangeIncomeCalculator(
            LocalCSVFilesTransactionProvider transactionProvider,
            RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;
        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public Map<String, ExchangeIncomeSummary> calculate(int year) {
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();
        List<PLNTransaction> plnTransactions = realizedTransactionCalculator.processForYear(ibTransactions, year);

        return plnTransactions.stream()
                .filter(t -> !t.isError())
                .collect(Collectors.toMap(
                        t -> {
                            String exch = t.getStatement().getListingExch();
                            if (exch == null) {
                                log.error("Missing listingExch for transaction: {}", t.getStatement());
                                return "UNKNOWN";
                            }
                            return exch;
                        },
                        t -> new ExchangeIncomeSummary(t.getIncome(), t.getCost()),
                        (a, b) -> new ExchangeIncomeSummary(a.income() + b.income(), a.cost() + b.cost())
                ));
    }
}
