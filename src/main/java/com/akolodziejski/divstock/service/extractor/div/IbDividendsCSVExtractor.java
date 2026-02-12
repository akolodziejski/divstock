package com.akolodziejski.divstock.service.extractor.div;

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
@Qualifier("ib-div")
public class IbDividendsCSVExtractor extends CSVExtractor {

    @Override
    protected List<Transaction> mapToTransactions(List<String[]> lines) {
        return lines.stream()
                .filter( row -> row[2].equals("Summary"))
                .map(this::mapToTransaction).collect(Collectors.toList());
    }


    protected Transaction mapToTransaction(String[] row) {

        return  Transaction.builder()
                .currency(row[3])
                .symbol(row[4])
                .date(parseDate(row[7]))
                .quantity(1)
                .price(Float.valueOf(row[12]))
                .build();
    }

    @SneakyThrows
    private Date parseDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
        return formatter.parse(date);
    }
}
