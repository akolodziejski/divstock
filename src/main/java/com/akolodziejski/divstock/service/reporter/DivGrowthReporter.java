package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DivGrowthReporter {


    private final LocalCSVFilesTransactionProvider transactionProvider;

    public DivGrowthReporter(LocalCSVFilesTransactionProvider transactionProvider) {
        this.transactionProvider = transactionProvider;
    }


    public void getGrowth() {

        List<Transaction> ibDividends = transactionProvider.getDegiroDividends();
        Map<String, List<Transaction>> collect = ibDividends.stream().collect(Collectors.groupingBy(Transaction::getSymbol));
    }
}
