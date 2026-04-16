package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.reporter.PLNTransaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SoldStocksReportWriter {

    public void write(List<PLNTransaction> transactions, int year) {
        write(transactions, year, "output");
    }

    public void write(List<PLNTransaction> transactions, int year, String outputDir) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("sold_stocks_" + year + ".csv");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file))) {
                pw.println("Symbol,Trade date,Quantity,Currency,Gain,Gain in PLN,Cost,Cost in PLN");
                for (PLNTransaction tx : transactions) {
                    pw.printf(Locale.US, "%s,%s,%.2f,%s,%.2f,%.2f,%.2f,%.2f%n",
                            tx.getStatement().getSymbol(),
                            dateFormat.format(tx.getStatement().getDate()),
                            Math.abs(tx.getStatement().getQuantity()),
                            tx.getStatement().getCurrency(),
                            tx.getGainLostInCurrency(),
                            tx.getGainLost(),
                            tx.getCostInCurrency(),
                            tx.getCost());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write sold stocks report for year " + year, e);
        }
    }
}
