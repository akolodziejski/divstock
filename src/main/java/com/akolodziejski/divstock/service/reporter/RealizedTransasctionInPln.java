package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.Common;
import com.akolodziejski.divstock.service.NbpPlnRateProvider;
import com.akolodziejski.divstock.service.TransactionsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.akolodziejski.divstock.service.Common.getPreviousWorkingDay;

//FIFO strategy
@Service
public class RealizedTransasctionInPln implements TransactionsProcessor<PLNTransaction> {

    private Logger log = LoggerFactory.getLogger(RealizedTransasctionInPln.class);

    private final NbpPlnRateProvider rateProvider;

    public RealizedTransasctionInPln(NbpPlnRateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    @Override
    public List<PLNTransaction> process(List<Transaction> transactions) {
        Map<String, List<Transaction>> symbolsToTrans = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.groupingBy(Transaction::getId));

        var result = new ArrayList<PLNTransaction>();
        for (String key: symbolsToTrans.keySet().stream().sorted().collect(Collectors.toList())) {
            result.addAll(processStock(symbolsToTrans.get(key)));
        }
        return result;
    }

    public List<PLNTransaction> processForYear(List<Transaction> allTrasations, int year) {
        List<PLNTransaction> transactions = process(allTrasations);
        List<PLNTransaction> inYearTransactions = transactions.stream()
                .filter(t -> t.getSellTransation().getDate().after(Common.getFirstDayForYear(year)))
                .filter(t -> t.getSellTransation().getDate().before(Common.getFirstDayForYear(year + 1)))
                .collect(Collectors.toList());

        for(PLNTransaction tran: inYearTransactions) {
            log.info("{}|{}", tran.getSellTransation().getSymbol(), tran.getGainLost());
        }

        return inYearTransactions;
    }


    private List<PLNTransaction> processStock( List<Transaction> transactions) {

        Queue<Transaction> queue = new LinkedList<>(transactions);
        Queue<Transaction> currentStateQueue = new LinkedList<>();

        var result = new ArrayList<PLNTransaction>();
        while(!queue.isEmpty()) {
            Transaction transaction = queue.poll();
            if(transaction.getQuantity() > 0) {
                //BUY
                currentStateQueue.add(transaction);
            } else if (transaction.getQuantity() < 0) {
                //SELL
                result.add(calculatePLNIncomeAndUpdateCurrent(transaction, currentStateQueue));
            }
        }
        return result;
    }

    private PLNTransaction calculatePLNIncomeAndUpdateCurrent(Transaction sellTransaction, Queue<Transaction> currentStateQueue) {

        float soldQuantity = Math.abs(sellTransaction.getQuantity());

        float totalGainLost = 0;
        float plnTotal = 0;


        while(soldQuantity > 0) {
            Transaction headTransaction = currentStateQueue.peek(); // DO NOT REMOVE YET

            if(headTransaction != null && headTransaction.getQuantity() > 0) {
                // BUY TRANS
                float takenStocks = 0;
                if(soldQuantity < headTransaction.getQuantity()) {
                    takenStocks = soldQuantity;
                    headTransaction.setQuantity(headTransaction.getQuantity() - soldQuantity);
                } else if ( soldQuantity >= headTransaction.getQuantity()) {
                    currentStateQueue.remove();

                    takenStocks = headTransaction.getQuantity();
                }

                soldQuantity-= takenStocks;
                //calculate gain/loss
                float buyCost = takenStocks * headTransaction.getPrice();
                float sellCost = takenStocks * sellTransaction.getPrice();
                float gainLoss =  sellCost - buyCost;
                totalGainLost += gainLoss;
                //PLN
                float plnBuyCost = buyCost * rateProvider.getRate(headTransaction.getCurrency(), getPreviousWorkingDay(headTransaction.getDate()));
                float plnSellCost = sellCost * rateProvider.getRate(sellTransaction.getCurrency(), getPreviousWorkingDay(sellTransaction.getDate()));
                float plnGainLost = plnSellCost - plnBuyCost;
                plnTotal += plnGainLost;

                log.debug(sellTransaction.getSymbol() + ": GAIN/LOSS = " + gainLoss);
            } else {
                log.warn(" SELL after SELL  calculations exception for " + sellTransaction.getSymbol());
                return PLNTransaction.builder()
                        .error(true)
                        .gainLost(0)
                        .sellTransation(sellTransaction)
                        .build();
            }
        }
       log.debug("{} - {}: {} pln ",sellTransaction.getDate(),  sellTransaction.getSymbol(), totalGainLost);

        return PLNTransaction.builder()
                .gainLost(plnTotal)
                .sellTransation(sellTransaction)
                .build();
    }
}
