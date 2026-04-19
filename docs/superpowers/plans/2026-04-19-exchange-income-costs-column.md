# Exchange Income Costs Column Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `Costs (PLN)` column to the exchange income CSV report by aggregating the PLN cost basis per listing exchange alongside the existing income aggregation.

**Architecture:** Introduce a `ExchangeIncomeSummary` record to carry both `income` and `cost` doubles, update `ExchangeIncomeCalculator` to return `Map<String, ExchangeIncomeSummary>`, update `ExchangeIncomeReportWriter` to write three columns, and fix the `DivstockApplication` call site.

**Tech Stack:** Java 17 records, Spring Boot, AssertJ + JUnit 5 tests, Gradle.

---

## File Map

| Action | File |
|--------|------|
| **Create** | `src/main/java/com/akolodziejski/divstock/model/reporter/ExchangeIncomeSummary.java` |
| **Modify** | `src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java` |
| **Modify** | `src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java` |
| **Modify** | `src/main/java/com/akolodziejski/divstock/DivstockApplication.java` |
| **Modify** | `src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java` |
| **Modify** | `src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java` |

---

### Task 1: Create `ExchangeIncomeSummary` record

**Files:**
- Create: `src/main/java/com/akolodziejski/divstock/model/reporter/ExchangeIncomeSummary.java`

- [ ] **Step 1: Create the record**

```java
package com.akolodziejski.divstock.model.reporter;

public record ExchangeIncomeSummary(double income, double cost) {}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/model/reporter/ExchangeIncomeSummary.java
git commit -m "feat: add ExchangeIncomeSummary record"
```

---

### Task 2: Update `ExchangeIncomeCalculator` to return income + cost

**Files:**
- Modify: `src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java`
- Modify: `src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java`

- [ ] **Step 1: Update the failing tests first**

Replace the entire content of `ExchangeIncomeCalculatorTest.java` with:

```java
package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ExchangeIncomeCalculatorTest {

    static class TestTransactionProvider extends LocalCSVFilesTransactionProvider {
        private final List<Transaction> testTransactions;

        TestTransactionProvider(List<Transaction> testTransactions) {
            super(null, null, null, null, null, null);
            this.testTransactions = testTransactions;
        }

        @Override
        public List<Transaction> getIbTransactions() {
            return testTransactions;
        }
    }

    static class TestRealizedTransactionCalculator extends RealizedTransasctionInPln {
        private final List<PLNTransaction> testTransactions;

        TestRealizedTransactionCalculator(List<PLNTransaction> testTransactions) {
            super(null);
            this.testTransactions = testTransactions;
        }

        @Override
        public List<PLNTransaction> processForYear(List<Transaction> ibTransactions, int year) {
            return testTransactions;
        }
    }

    private ExchangeIncomeCalculator calculator;

    private void setUp(List<PLNTransaction> plnTransactions) {
        var transactionProvider = new TestTransactionProvider(List.of());
        var realizedTransactionCalculator = new TestRealizedTransactionCalculator(plnTransactions);
        calculator = new ExchangeIncomeCalculator(transactionProvider, realizedTransactionCalculator);
    }

    private PLNTransaction plnTx(String exchange, float income, float cost) {
        Transaction statement = Transaction.builder()
                .symbol("AAPL").currency("USD").listingExch(exchange).build();
        return PLNTransaction.builder()
                .income(income).cost(cost).gainLost(0).error(false).statement(statement).build();
    }

    private PLNTransaction errorTx(String exchange) {
        Transaction statement = Transaction.builder()
                .symbol("ERR").currency("USD").listingExch(exchange).build();
        return PLNTransaction.builder()
                .income(999).cost(500).gainLost(0).error(true).statement(statement).build();
    }

    @Test
    void groups_income_and_cost_by_exchange() {
        setUp(List.of(
                plnTx("NYSE", 1000f, 800f),
                plnTx("NYSE", 500f, 400f),
                plnTx("TSE", 200f, 150f)
        ));

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result.get("NYSE").income()).isCloseTo(1500.0, within(0.01));
        assertThat(result.get("NYSE").cost()).isCloseTo(1200.0, within(0.01));
        assertThat(result.get("TSE").income()).isCloseTo(200.0, within(0.01));
        assertThat(result.get("TSE").cost()).isCloseTo(150.0, within(0.01));
    }

    @Test
    void error_transactions_are_excluded() {
        setUp(List.of(plnTx("NYSE", 1000f, 800f), errorTx("NYSE")));

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result.get("NYSE").income()).isCloseTo(1000.0, within(0.01));
        assertThat(result.get("NYSE").cost()).isCloseTo(800.0, within(0.01));
    }

    @Test
    void null_listingExch_grouped_under_unknown() {
        Transaction statement = Transaction.builder()
                .symbol("XYZ").currency("USD").listingExch(null).build();
        PLNTransaction nullExchange = PLNTransaction.builder()
                .income(300f).cost(250f).gainLost(0).error(false).statement(statement).build();

        setUp(List.of(nullExchange));

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result.get("UNKNOWN").income()).isCloseTo(300.0, within(0.01));
        assertThat(result.get("UNKNOWN").cost()).isCloseTo(250.0, within(0.01));
    }

    @Test
    void empty_transactions_returns_empty_map() {
        setUp(List.of());

        Map<String, ExchangeIncomeSummary> result = calculator.calculate(2024);

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculatorTest"
```
Expected: FAILED (compile error or assertion mismatch — `calculate()` still returns `Map<String, Double>`)

- [ ] **Step 3: Update `ExchangeIncomeCalculator`**

Replace the entire content of `ExchangeIncomeCalculator.java` with:

```java
package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExchangeIncomeCalculator {

    private static final Logger log = LoggerFactory.getLogger(ExchangeIncomeCalculator.class);

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public ExchangeIncomeCalculator(
            LocalCSVFilesTransactionProvider transactionProvider,
            RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;
        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public Map<String, ExchangeIncomeSummary> calculate(int year) {
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();
        List<PLNTransaction> plnTransactions = realizedTransactionCalculator.processForYear(ibTransactions, year);

        return plnTransactions.stream()
                .filter(t -> !t.isError())
                .collect(Collectors.toMap(
                        t -> {
                            String exch = t.getStatement().getListingExch();
                            if (exch == null) {
                                log.error("Missing listingExch for transaction: {}", t.getStatement());
                                return "UNKNOWN";
                            }
                            return exch;
                        },
                        t -> new ExchangeIncomeSummary(t.getIncome(), t.getCost()),
                        (a, b) -> new ExchangeIncomeSummary(a.income() + b.income(), a.cost() + b.cost())
                ));
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculatorTest"
```
Expected: `BUILD SUCCESSFUL`, 4 tests passing

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java \
        src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java
git commit -m "feat: ExchangeIncomeCalculator returns income and cost per exchange"
```

---

### Task 3: Update `ExchangeIncomeReportWriter` to write three columns

**Files:**
- Modify: `src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java`
- Modify: `src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java`

- [ ] **Step 1: Update the failing tests first**

Replace the entire content of `ExchangeIncomeReportWriterTest.java` with:

```java
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
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriterTest"
```
Expected: FAILED (compile error — `write()` still accepts `Map<String, Double>`)

- [ ] **Step 3: Update `ExchangeIncomeReportWriter`**

Replace the entire content of `ExchangeIncomeReportWriter.java` with:

```java
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
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriterTest"
```
Expected: `BUILD SUCCESSFUL`, 4 tests passing

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java \
        src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java
git commit -m "feat: ExchangeIncomeReportWriter writes Income and Costs columns"
```

---

### Task 4: Fix `DivstockApplication` call site and run full test suite

**Files:**
- Modify: `src/main/java/com/akolodziejski/divstock/DivstockApplication.java`

- [ ] **Step 1: Update the `run` method**

The `exchangeIncomeCalculator.calculate(year)` call now returns `Map<String, ExchangeIncomeSummary>`. Update the import and the local variable type. Replace lines 3–8 and 45–46 so the file reads:

```java
package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.reporter.ExchangeIncomeSummary;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriter;
import com.akolodziejski.divstock.service.reporter.SoldStocksReportWriter;
import com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculator;
import com.akolodziejski.divstock.service.tax.PIT38Calculator;
import com.akolodziejski.divstock.service.tax.SoldStocksCalculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.util.List;
import java.util.Map;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication implements CommandLineRunner {

    private final PIT38Calculator calculator;
    private final ExchangeIncomeCalculator exchangeIncomeCalculator;
    private final SoldStocksCalculator soldStocksCalculator;

    public DivstockApplication(PIT38Calculator calculator,
                                ExchangeIncomeCalculator exchangeIncomeCalculator,
                                SoldStocksCalculator soldStocksCalculator) {
        this.calculator = calculator;
        this.exchangeIncomeCalculator = exchangeIncomeCalculator;
        this.soldStocksCalculator = soldStocksCalculator;
    }

    public static void main(String[] args) {
        SpringApplication.run(DivstockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        int year = 2025;

        Profit profit = calculator.calculate(year);
        System.out.println(profit);

        Map<String, ExchangeIncomeSummary> exchangeIncome = exchangeIncomeCalculator.calculate(year);
        new ExchangeIncomeReportWriter().write(exchangeIncome, year);
        System.out.println("Exchange income report written to output/exchange_income_" + year + ".csv");

        List<PLNTransaction> soldStocks = soldStocksCalculator.calculate(year);
        new SoldStocksReportWriter().write(soldStocks, year);
        System.out.println("Sold stocks report written to output/sold_stocks_" + year + ".csv");
    }
}
```

- [ ] **Step 2: Run the full test suite**

```bash
./gradlew test
```
Expected: `BUILD SUCCESSFUL`, all tests passing

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/DivstockApplication.java
git commit -m "feat: wire ExchangeIncomeSummary through DivstockApplication"
```
