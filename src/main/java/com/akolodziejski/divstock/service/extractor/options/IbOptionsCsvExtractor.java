package com.akolodziejski.divstock.service.extractor.options;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.extractor.CSVExtractor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Qualifier("options")
public class IbOptionsCsvExtractor  extends CSVExtractor {

    //Trades,Data,Order,Equity and Index Options,USD,AQN 21JAN22 15.0 C,"2021-10-11, 11:28:44",-1,0.38,38,-0.1945438,
    @Override
    protected List<Transaction> mapToTransactions(List<String[]> lines) {
        return lines.stream()
                .filter( row -> row[2].equals("Order"))
                .filter(row ->row[3].equals("Equity and Index Options"))
                .map(this::mapToTransaction).collect(Collectors.toList());
    }

    protected Transaction mapToTransaction(String[] row) {

        return  Transaction.builder()
                .currency(row[4])
                .symbol(row[5])
                .date(parseDate(row[6]))
                .quantity(Float.valueOf(row[7]))
                .price(Float.valueOf(row[8]))
                .proceeds(Float.valueOf(row[9]))
                .fee(Float.valueOf(row[10])).build();
    }

    @SneakyThrows
    private Date parseDate(String dateTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String datePart = dateTime.split(",")[0];

        if(datePart == null || datePart.isBlank())
            return null;
        return formatter.parse(datePart);
    }
}
