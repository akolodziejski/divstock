package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.reporter.RealizedTransasctionInPln;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import java.util.List;

@Service
public class PIT38TaxCalculator {

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public PIT38TaxCalculator(
            LocalCSVFilesTransactionProvider transactionProvider, RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;

        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public Profit calculate(int year) {
        List<Transaction> degiroTransactions = transactionProvider.getDegiroTransactions();
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();


        List<PLNTransaction> degiroPlnTransactions = realizedTransactionCalculator.processForYear(degiroTransactions, 2021);
        List<PLNTransaction> ibPlnTransactions = realizedTransactionCalculator.processForYear(ibTransactions, 2021);

        List<PLNTransaction> combinedTrans = new ArrayList<>(degiroPlnTransactions);
        combinedTrans.addAll(ibPlnTransactions);

        double degiroSum = degiroPlnTransactions.stream().mapToDouble(PLNTransaction::getGainLost).sum();
        double ibSum = ibPlnTransactions.stream().mapToDouble(PLNTransaction::getGainLost).sum();

        double totalIncome = combinedTrans.stream().mapToDouble(PLNTransaction::getIncome).sum();
        double totalCost = combinedTrans.stream().mapToDouble(PLNTransaction::getCost).sum();

        return Profit.builder().income(totalIncome).taxPaid(0).cost(totalCost).build();
    }


}
