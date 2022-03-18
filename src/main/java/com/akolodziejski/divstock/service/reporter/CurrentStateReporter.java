package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.NbpPlnRateProvider;
import com.akolodziejski.divstock.service.TransactionsProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurrentStateReporter implements TransactionsProcessor<Void> {

    private final NbpPlnRateProvider rateProvider;

    public CurrentStateReporter(NbpPlnRateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    @Override
    public List<Void> process(List<Transaction> transactions) {
        Map<String, List<Transaction>> symbolsToTrans = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.groupingBy(Transaction::getId));

        for (String key: symbolsToTrans.keySet().stream().sorted().collect(Collectors.toList())) {
            processStock(key, symbolsToTrans.get(key));
        }
        return new ArrayList<>();
    }

    private void processStock(String ticker, List<Transaction> transactions) {

        float currentQuantity = 0;
        float sumProceed = 0;
        float avgPrice = 0;
        Queue<Transaction> queue = new LinkedList<>(transactions);
        while(!queue.isEmpty()) {
            Transaction transaction = queue.poll();

            currentQuantity += transaction.getQuantity();
            sumProceed += transaction.getProceeds();
        }

        if(currentQuantity > 0) {
            System.out.print( ticker);
            System.out.print(": " + currentQuantity);
            System.out.println(" avg" + sumProceed/currentQuantity + "$");
        }
    }
}
