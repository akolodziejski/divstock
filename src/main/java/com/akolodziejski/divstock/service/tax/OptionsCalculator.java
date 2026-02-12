package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.Common;
import com.akolodziejski.divstock.service.NbpPlnRateProvider;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OptionsCalculator {

    //TODO consider removing this
    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final NbpPlnRateProvider rateProvider;


    public OptionsCalculator(LocalCSVFilesTransactionProvider transactionProvider, NbpPlnRateProvider rateProvider) {
        this.transactionProvider = transactionProvider;
        this.rateProvider = rateProvider;
    }

    public Profit calculate(int year) {
        List<Transaction> optionsTransactions = filterForYear(year, transactionProvider.getOptionsTransactions());
        List<PLNTransaction> plnTransactions = process(optionsTransactions);

        double income = plnTransactions.stream().mapToDouble(PLNTransaction::getIncome).sum();
        double cost = plnTransactions.stream().mapToDouble(PLNTransaction::getCost).sum();

        return Profit.builder()
                .income(income)
                .cost(cost)
                .build();
    }

    private List<PLNTransaction> process(List<Transaction> optionsTransactions) {

        Map<String, List<Transaction>> mapBySymbol
                = optionsTransactions.stream().collect(Collectors.groupingBy(Transaction::getSymbol));

        var result = new ArrayList<PLNTransaction>();
        for (String symbolKey : mapBySymbol.keySet()) {
            result.add(processForSymbol(mapBySymbol.get(symbolKey)));
        }
        return result;
    }

    private PLNTransaction processForSymbol(List<Transaction> transactions) {

        //FIXME: implement more complicated cases
        if(transactions.size() != 2) {
            return PLNTransaction.builder().build();
        }

        Transaction sell = transactions.get(0);
        Transaction buy = transactions.get(1); // later


        PLNTransaction sellPln = PLNTransaction.builder()
                .income(sell.getProceeds() * rateProvider.getRate(sell.getCurrency(), sell.getDate()))
                .build();

        PLNTransaction buyPln = PLNTransaction.builder()
                .cost(buy.getProceeds() * rateProvider.getRate(sell.getCurrency(), sell.getDate()))
                .build();


        return PLNTransaction.builder()
                .income(sellPln.getIncome())
                .cost(Math.abs(buyPln.getCost()))
                .build();
    }

    private List<Transaction> filterForYear(int year, List<Transaction> degiroDividends) {
        return degiroDividends.stream()
                .filter(t -> t.getDate().after(Common.getFirstDayForYear(year)))
                .filter(t -> t.getDate().before(Common.getFirstDayForYear(year + 1)))
                .collect(Collectors.toList());
    }
}
