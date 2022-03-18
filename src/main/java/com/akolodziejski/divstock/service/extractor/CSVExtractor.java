package com.akolodziejski.divstock.service.extractor;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CSVExtractor {

    protected int getSkipLines() {
        return 1;
    }

    private CSVReader getReader(String csvFile) throws IOException {
        Reader reader = Files.newBufferedReader(Path.of(csvFile));
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(getSkipLines())
                .withCSVParser(csvParser)
                .build();
        return csvReader;
    }


    public List<Transaction> getTransactions(List<String> csvFiles) {
        return csvFiles.stream().map(this::getTransactions).flatMap(List::stream).collect(Collectors.toList());
    }

    @SneakyThrows
    private List<Transaction> getTransactions(String csvFile) {
        return mapToTransactions(getReader(csvFile).readAll());
    }

    protected List<Transaction> mapToTransactions(List<String[]> lines) {
        return lines.stream()
                .map(this::mapToTransaction).collect(Collectors.toList());
    }

    protected abstract Transaction mapToTransaction(String[] row);
}
