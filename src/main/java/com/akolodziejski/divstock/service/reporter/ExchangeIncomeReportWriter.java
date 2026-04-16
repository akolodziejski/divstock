package com.akolodziejski.divstock.service.reporter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class ExchangeIncomeReportWriter {

    public void write(Map<String, Double> exchangeIncomeMap, int year) {
        write(exchangeIncomeMap, year, "output");
    }

    public void write(Map<String, Double> exchangeIncomeMap, int year, String outputDir) {
        try {
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("exchange_income_" + year + ".csv");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file))) {
                pw.println("Listing Exch,Income (PLN)");
                exchangeIncomeMap.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> pw.printf(Locale.US, "%s,%.2f%n", e.getKey(), e.getValue()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write exchange income report for year " + year, e);
        }
    }
}
