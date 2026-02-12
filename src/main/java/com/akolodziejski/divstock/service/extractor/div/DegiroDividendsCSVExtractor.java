package com.akolodziejski.divstock.service.extractor.div;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.extractor.CSVExtractor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@Qualifier("degiro-div")
public class DegiroDividendsCSVExtractor extends CSVExtractor {
    @Override
    protected List<Transaction> mapToTransactions(List<String[]> lines) {
        return lines.stream()
                .filter( row -> row[5].equals("Dywidenda"))
                .map(this::mapToTransaction).collect(Collectors.toList());
    }


    protected Transaction mapToTransaction(String[] row) {

        return  Transaction.builder()
                .currency(row[7])
                .symbol(row[4])
                .date(parseDate(row[0]))
                .quantity(1)
                .price(Float.valueOf(row[8].replace(',', '.'))).build();
    }

    @SneakyThrows
    private Date parseDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        return formatter.parse(date);
    }
}
