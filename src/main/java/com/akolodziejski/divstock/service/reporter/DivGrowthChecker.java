package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DivGrowthChecker {

    private Logger log = LoggerFactory.getLogger(DivGrowthChecker.class);

    private final LocalCSVFilesTransactionProvider transactionProvider;

    public DivGrowthChecker(LocalCSVFilesTransactionProvider transactionProvider) {
        this.transactionProvider = transactionProvider;
    }

    public void stockDetails(String symbol) {
        List<Transaction> all = transactionProvider.getDegiroTransactions();
        List<Transaction> ibDividends = transactionProvider.getIbTransactions();
        all.addAll(ibDividends);

        List<Transaction> filtered = all.stream().filter(t -> symbol.equalsIgnoreCase(t.getSymbol())).collect(Collectors.toList());

        for (Transaction t : filtered) {
            log.info("DETAILS {}|{}|{}", t.getProceeds(), t.getQuantity(), t.getDate());
        }

    }


    public void getGrowth() {

        List<Transaction> all = getAllBuySell();

        Map<String, List<Transaction>> bySymbol = all.stream().collect(Collectors.groupingBy(Transaction::getSymbol));

        for (Map.Entry<String, List<Transaction>> entry: bySymbol.entrySet()) {
            System.out.print(entry.getKey()+ ": ");
            entry.getValue().stream().collect(Collectors.groupingBy(i -> i.getDate().getYear()))
                    .forEach( (year, trans) -> {
                        Transaction maxInYear = trans.stream().max( (t1, t2) -> t1.getPrice() > t2.getPrice()? 1:-1).get();
                        System.out.print(maxInYear.getPrice() + " ");
                    });
            System.out.println("");
        }

    }

    @NotNull
    private List<Transaction> getAllBuySell() {
        List<Transaction> all = transactionProvider.getDegiroDividends();
        List<Transaction> ibDividends = transactionProvider.getIbDividends();
        all.addAll(ibDividends);
        return all;
    }

    public void disctinctSymbols() {
        List<Transaction> all = getAllBuySell();
        all.stream().map(t -> t.getSymbol()).distinct().collect(Collectors.toSet()).forEach( s -> System.out.println("SYMBOL " + s));
    }
}
