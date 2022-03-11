package com.akolodziejski.divstock.service;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class TransactionsCSVExtractor {

    public List<Transaction> getTransactions(List<String> csvFiles) {
        return csvFiles.stream().map(this::getTransactions).flatMap(List::stream).collect(Collectors.toList());
    }

    @SneakyThrows
    private List<Transaction> getTransactions(String csvFile) {
        return mapToTransactions(getReader(csvFile).readAll());
    }

    private CSVReader getReader(String csvFile) throws IOException {
        Reader reader = Files.newBufferedReader(Path.of(csvFile));
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(7)
                .withCSVParser(csvParser)
                .build();
        return csvReader;
    }

    private List<Transaction> mapToTransactions(List<String[]> lines) {
        return lines.stream()
                .filter( row -> row[2].equals("Order"))
                .filter(row ->row[3].equals("Stocks"))
                .map(this::mapToTransaction).collect(Collectors.toList());
    }

    private Transaction mapToTransaction(String[] row) {

        return  Transaction.builder()
                //      1 = "Header"
                .header( row[1])
                //        2 = "DataDiscriminator"
                .dataDiscriminator(row[2])
                //        3 = "Asset Category"
                .assetCategory(row[3])
                //        4 = "Currency"
                .currency(row[4])
                //        5 = "Symbol"
                .symbol(row[5])
                //        6 = "Date/Time"
                .date(parseDate(row[6]))
                //        7 = "Quantity"
                .quantity(Float.valueOf(row[7]))
                //        8 = "T. Price"
                .price(Float.valueOf(row[8]))
                //        9 = "Proceeds"
                .proceeds(Float.valueOf(row[9]))
                //        10 = "Comm/Fee"
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
