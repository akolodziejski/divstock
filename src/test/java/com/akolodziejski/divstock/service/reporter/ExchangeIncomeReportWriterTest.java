package com.akolodziejski.divstock.service.reporter;

import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeIncomeReportWriterTest {

    private final ExchangeIncomeReportWriter writer = new ExchangeIncomeReportWriter();

    @TempDir
    Path tempDir;

    @Test
    void writes_header_and_rows() throws IOException {
        Map<String, ExchangeIncomeSummary> data = Map.of(
                "NYSE", new ExchangeIncomeSummary(12345.678, 10000.0)
        );

        writer.write(data, 2024, tempDir.toString());

        Path output = tempDir.resolve("exchange_income_2024.csv");
        assertThat(output).exists();
        List<String> lines = Files.readAllLines(output);
        assertThat(lines.get(0)).isEqualTo("Listing Exch,Income (PLN),Costs (PLN)");
    }

    @Test
    void rows_are_sorted_alphabetically_by_exchange() throws IOException {
        Map<String, ExchangeIncomeSummary> data = Map.of(
                "TSE", new ExchangeIncomeSummary(200.0, 150.0),
                "NYSE", new ExchangeIncomeSummary(1000.0, 800.0),
                "AEB", new ExchangeIncomeSummary(500.0, 400.0)
        );

        writer.write(data, 2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("exchange_income_2024.csv"));
        assertThat(lines.get(1)).startsWith("AEB,");
        assertThat(lines.get(2)).startsWith("NYSE,");
        assertThat(lines.get(3)).startsWith("TSE,");
    }

    @Test
    void income_and_cost_formatted_to_two_decimal_places() throws IOException {
        Map<String, ExchangeIncomeSummary> data = Map.of(
                "NYSE", new ExchangeIncomeSummary(12345.678, 9999.999)
        );

        writer.write(data, 2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("exchange_income_2024.csv"));
        assertThat(lines.get(1)).isEqualTo("NYSE,12345.68,10000.00");
    }

    @Test
    void output_directory_is_created_if_missing() throws IOException {
        Path nested = tempDir.resolve("nested/output");
        Map<String, ExchangeIncomeSummary> data = Map.of(
                "NYSE", new ExchangeIncomeSummary(100.0, 80.0)
        );

        writer.write(data, 2025, nested.toString());

        assertThat(nested.resolve("exchange_income_2025.csv")).exists();
    }
}
