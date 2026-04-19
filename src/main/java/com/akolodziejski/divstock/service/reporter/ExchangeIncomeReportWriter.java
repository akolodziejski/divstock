package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class ExchangeIncomeReportWriter {

    public void write(Map<String, ExchangeIncomeSummary> exchangeMap, int year) {
        write(exchangeMap, year, "output");
    }

    public void write(Map<String, ExchangeIncomeSummary> exchangeMap, int year, String outputDir) {
        try {
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("exchange_income_" + year + ".csv");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file))) {
                pw.println("Listing Exch,Income (PLN),Costs (PLN)");
                exchangeMap.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> pw.printf(Locale.US, "%s,%.2f,%.2f%n",
                                e.getKey(), e.getValue().income(), e.getValue().cost()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write exchange income report for year " + year, e);
        }
    }
}
