package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SoldStocksReportWriterTest {

    private final SoldStocksReportWriter writer = new SoldStocksReportWriter();

    @TempDir
    Path tempDir;

    private Date date(String s) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse(s);
    }

    private PLNTransaction tx(String symbol, String currency, float qty,
                               Date tradeDate, float costInCurrency,
                               float gainLostInCurrency, float cost, float gainLost) {
        Transaction statement = Transaction.builder()
                .symbol(symbol).currency(currency)
                .quantity(qty).date(tradeDate).build();
        return PLNTransaction.builder()
                .statement(statement)
                .costInCurrency(costInCurrency)
                .gainLostInCurrency(gainLostInCurrency)
                .cost(cost)
                .gainLost(gainLost)
                .error(false)
                .build();
    }

    @Test
    void writes_correct_header() throws IOException, ParseException {
        writer.write(List.of(), 2024, tempDir.toString());

        Path file = tempDir.resolve("sold_stocks_2024.csv");
        assertThat(file).exists();
        List<String> lines = Files.readAllLines(file);
        assertThat(lines.get(0))
                .isEqualTo("Symbol,Trade date,Quantity,Currency,Gain,Gain in PLN,Cost,Cost in PLN");
    }

    @Test
    void writes_one_row_per_transaction() throws IOException, ParseException {
        List<PLNTransaction> txs = List.of(
                tx("AAPL", "USD", -10f, date("2024-03-15"), 1000f, 200f, 4000f, 800f),
                tx("GOOG", "USD", -5f,  date("2024-06-01"), 2000f, -100f, 8000f, -400f)
        );

        writer.write(txs, 2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("sold_stocks_2024.csv"));
        assertThat(lines).hasSize(3); // header + 2 rows
    }

    @Test
    void quantity_is_absolute_value() throws IOException, ParseException {
        writer.write(
                List.of(tx("AAPL", "USD", -50f, date("2024-03-15"), 500f, 100f, 2000f, 400f)),
                2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("sold_stocks_2024.csv"));
        String row = lines.get(1);
        assertThat(row.split(",")[2]).isEqualTo("50.00");
    }

    @Test
    void numbers_formatted_to_two_decimal_places() throws IOException, ParseException {
        writer.write(
                List.of(tx("BMY", "USD", -100f, date("2024-03-15"), 1234.567f, 89.123f, 4938.268f, 356.492f)),
                2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("sold_stocks_2024.csv"));
        String row = lines.get(1);
        String[] cols = row.split(",");
        assertThat(cols[4]).isEqualTo("89.12");    // Gain
        assertThat(cols[5]).isEqualTo("356.49");   // Gain in PLN
        assertThat(cols[6]).isEqualTo("1234.57");  // Cost
        assertThat(cols[7]).isEqualTo("4938.27");  // Cost in PLN
    }

    @Test
    void date_formatted_as_yyyy_MM_dd() throws IOException, ParseException {
        writer.write(
                List.of(tx("AAPL", "USD", -10f, date("2024-03-05"), 100f, 10f, 400f, 40f)),
                2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("sold_stocks_2024.csv"));
        String row = lines.get(1);
        assertThat(row.split(",")[1]).isEqualTo("2024-03-05");
    }

    @Test
    void output_directory_is_created_if_missing() throws IOException, ParseException {
        Path nested = tempDir.resolve("nested/output");

        writer.write(List.of(), 2025, nested.toString());

        assertThat(nested.resolve("sold_stocks_2025.csv")).exists();
    }
}
