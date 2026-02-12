package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.Common;
import com.akolodziejski.divstock.service.NbpPlnRateProvider;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DividendsCalculator {

    //TODO consider removing this
    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final NbpPlnRateProvider rateProvider;
    private final TaxRateService taxRateService;

    public DividendsCalculator(LocalCSVFilesTransactionProvider transactionProvider, NbpPlnRateProvider rateProvider, TaxRateService taxRateService) {
        this.transactionProvider = transactionProvider;
        this.rateProvider = rateProvider;
        this.taxRateService = taxRateService;
    }

    public Profit calculate(int year) {
        List<Transaction> allTrans = filterForYear(year, transactionProvider.getDegiroDividends());
        List<Transaction> ibDivs = filterForYear(year, transactionProvider.getIbDividends());

        allTrans.addAll(ibDivs);

        List<PLNTransaction> plnTransactions = getInPln(allTrans);

        double income = plnTransactions.stream().mapToDouble(PLNTransaction::getIncome).sum();
        double taxPaid = plnTransactions.stream().mapToDouble(PLNTransaction::getTaxPaid).sum();

        return Profit.builder()
                .income(income)
                .taxPaid(taxPaid)
                .cost(0.0)
                .build();
    }

    private List<PLNTransaction> getInPln(List<Transaction> yearDivs) {
       return yearDivs.stream().map( t ->
               {
                   float currencyRate = rateProvider.getRate(t.getCurrency(), t.getDate());
                   float income = t.getPrice() * currencyRate;
                   return PLNTransaction.builder()
                           .income(income)
                           .cost(0)
                           .taxPaid(income * taxRateService.getRate(t.getSymbol()))
                           .statement(t)
                   .build();
               }
       ).collect(Collectors.toList());
    }

    private List<Transaction> filterForYear(int year, List<Transaction> degiroDividends) {
        return degiroDividends.stream()
                .filter(t -> t.getDate().after(Common.getFirstDayForYear(year)))
                .filter(t -> t.getDate().before(Common.getFirstDayForYear(year + 1)))
                .collect(Collectors.toList());
    }
}
