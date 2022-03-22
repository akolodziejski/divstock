package com.akolodziejski.divstock.service;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.extractor.CSVExtractor;
import com.akolodziejski.divstock.service.reporter.RealizedTransasctionInPln;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalCSVFilesTransactionProvider {

    private final CSVExtractor degiroTransactionsExtractor;
    private final CSVExtractor ibTransactionsExtractor;

    private final StatementsFileService statementsFileService;

    public LocalCSVFilesTransactionProvider(
            @Qualifier("degiro") CSVExtractor degiroTransactionsExtractor,
            @Qualifier("ib") CSVExtractor ibTransactionsExtractor,
            StatementsFileService statementsFileService) {
        this.degiroTransactionsExtractor = degiroTransactionsExtractor;
        this.ibTransactionsExtractor = ibTransactionsExtractor;
        this.statementsFileService = statementsFileService;
    }

    public List<Transaction> getDegiroTransactions() {
        return degiroTransactionsExtractor.getTransactions(statementsFileService.getDegiroTransactions());
    }
    public List<Transaction> getIbTransactions() {
        return ibTransactionsExtractor.getTransactions(statementsFileService.getIbTransactions());
    }
}
