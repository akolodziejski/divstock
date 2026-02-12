package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RealizedTransactionsCalculator {

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public RealizedTransactionsCalculator(
            LocalCSVFilesTransactionProvider transactionProvider, RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;

        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public Profit calculate(int year) {
        List<Transaction> degiroTransactions = transactionProvider.getDegiroTransactions();
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();

        List<PLNTransaction> degiroPlnTransactions = realizedTransactionCalculator.processForYear(degiroTransactions, year);
        List<PLNTransaction> ibPlnTransactions = realizedTransactionCalculator.processForYear(ibTransactions, year);

        List<PLNTransaction> combinedTrans = new ArrayList<>(degiroPlnTransactions);
        combinedTrans.addAll(ibPlnTransactions);
        
        // errors
        List<PLNTransaction> nonError = combinedTrans.stream().filter(t -> !t.isError()).collect(Collectors.toList());

        double totalIncome = nonError.stream().mapToDouble(PLNTransaction::getIncome).sum();
        double totalCost = nonError.stream().mapToDouble(PLNTransaction::getCost).sum();
        double totalGainLostSum = nonError.stream().mapToDouble(PLNTransaction::getGainLost).sum();

        return Profit.builder().income(totalIncome).taxPaid(0).cost(totalCost).build();
    }


}
