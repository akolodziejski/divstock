package com.akolodziejski.divstock.service.transaction;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.extractor.CSVExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalCSVFilesTransactionProvider {

    private final CSVExtractor degiroTransactionsExtractor;
    private final CSVExtractor degiroDivExtractor;
    private final CSVExtractor ibDivExtractor;
    private final CSVExtractor ibTransactionsExtractor;
    private final CSVExtractor optionsExtractor;

    private final StatementsFileService statementsFileService;

    public LocalCSVFilesTransactionProvider(
            @Qualifier("degiro") CSVExtractor degiroTransactionsExtractor,
            @Qualifier("degiro-div") CSVExtractor degiroDivExtractor,
            @Qualifier("ib-div") CSVExtractor ibDivExtractor,
            @Qualifier("ib") CSVExtractor ibTransactionsExtractor,
            @Qualifier("options") CSVExtractor optionsExtractor,
            StatementsFileService statementsFileService) {
        this.degiroTransactionsExtractor = degiroTransactionsExtractor;
        this.degiroDivExtractor = degiroDivExtractor;
        this.ibDivExtractor = ibDivExtractor;
        this.ibTransactionsExtractor = ibTransactionsExtractor;
        this.optionsExtractor = optionsExtractor;
        this.statementsFileService = statementsFileService;
    }

    public List<Transaction> getDegiroTransactions() {
        return degiroTransactionsExtractor.getTransactions(statementsFileService.getDegiroTransactions());
    }

    public List<Transaction> getDegiroDividends() {
        return degiroDivExtractor.getTransactions(statementsFileService.getDegiroDividends());
    }

    public List<Transaction> getIbTransactions() {
        return ibTransactionsExtractor.getTransactions(statementsFileService.getIbTransactions());
    }

    public List<Transaction> getIbDividends() {
        return ibDivExtractor.getTransactions(statementsFileService.getIbDividends());
    }

    public List<Transaction> getOptionsTransactions() {
        return optionsExtractor.getTransactions(statementsFileService.getIbTransactions());
    }

}
