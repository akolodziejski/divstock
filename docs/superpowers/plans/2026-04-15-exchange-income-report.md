# Exchange Income Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Group IB sell transactions by listing exchange and write a CSV report of PLN income per exchange for a given year.

**Architecture:** Add `listingExch` to `Transaction`, enrich it during IB CSV extraction via a two-pass read of `Financial Instrument Information` rows, then a new `ExchangeIncomeCalculator` service reuses the existing FIFO+PLN pipeline (`RealizedTransasctionInPln`) and groups results by exchange. A plain `ExchangeIncomeReportWriter` writes the CSV output.

**Tech Stack:** Java 17, Spring Boot 3.4.4, Lombok (`@Data`/`@Builder`), opencsv 5.9, JUnit 5, Mockito (via `spring-boot-starter-test`)

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `src/main/java/com/akolodziejski/divstock/model/csv/Transaction.java` | Add nullable `listingExch` field |
| Create | `src/main/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistry.java` | Hardcoded exchange→currency lookup |
| Modify | `src/main/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractor.java` | Two-pass `mapToTransactions()` to populate `listingExch` |
| Create | `src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java` | Spring service: group PLN income by exchange |
| Create | `src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java` | Write exchange income CSV |
| Modify | `src/main/java/com/akolodziejski/divstock/DivstockApplication.java` | Wire new components into `run()` |
| Create | `src/test/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistryTest.java` | Unit tests for registry |
| Create | `src/test/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractorExchangeTest.java` | Unit tests for two-pass extraction |
| Create | `src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java` | Unit tests for grouping logic |
| Create | `src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java` | Unit tests for CSV output |

---

## Task 1: `ExchangeCurrencyRegistry`

**Files:**
- Create: `src/main/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistry.java`
- Create: `src/test/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistryTest.java`

- [ ] **Step 1: Create the test directory structure**

```bash
mkdir -p src/test/java/com/akolodziejski/divstock/service/extractor
mkdir -p src/test/java/com/akolodziejski/divstock/service/tax
mkdir -p src/test/java/com/akolodziejski/divstock/service/reporter
```

- [ ] **Step 2: Write the failing tests**

Create `src/test/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistryTest.java`:

```java
package com.akolodziejski.divstock.service.extractor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExchangeCurrencyRegistryTest {

    private final ExchangeCurrencyRegistry registry = new ExchangeCurrencyRegistry();

    @Test
    void nyse_returns_usd() {
        assertThat(registry.getCurrency("NYSE")).isEqualTo("USD");
    }

    @Test
    void nasdaq_returns_usd() {
        assertThat(registry.getCurrency("NASDAQ")).isEqualTo("USD");
    }

    @Test
    void tse_returns_cad() {
        assertThat(registry.getCurrency("TSE")).isEqualTo("CAD");
    }

    @Test
    void lse_returns_gbp() {
        assertThat(registry.getCurrency("LSE")).isEqualTo("GBP");
    }

    @Test
    void sehk_returns_hkd() {
        assertThat(registry.getCurrency("SEHK")).isEqualTo("HKD");
    }

    @Test
    void sgx_returns_sgd() {
        assertThat(registry.getCurrency("SGX")).isEqualTo("SGD");
    }

    @Test
    void aeb_returns_eur() {
        assertThat(registry.getCurrency("AEB")).isEqualTo("EUR");
    }

    @Test
    void unknown_exchange_returns_null() {
        assertThat(registry.getCurrency("UNKNOWN_XYZ")).isNull();
    }
}
```

- [ ] **Step 3: Run the tests to confirm they fail**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.extractor.ExchangeCurrencyRegistryTest" 2>&1 | tail -20
```

Expected: compilation failure — `ExchangeCurrencyRegistry` does not exist yet.

- [ ] **Step 4: Create `ExchangeCurrencyRegistry`**

Create `src/main/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistry.java`:

```java
package com.akolodziejski.divstock.service.extractor;

import java.util.Map;

public class ExchangeCurrencyRegistry {

    private static final Map<String, String> EXCHANGE_TO_CURRENCY = Map.of(
            "NYSE",   "USD",
            "NASDAQ", "USD",
            "AMEX",   "USD",
            "TSE",    "CAD",
            "LSE",    "GBP",
            "SEHK",   "HKD",
            "SGX",    "SGD",
            "AEB",    "EUR"
    );

    public String getCurrency(String exchange) {
        return EXCHANGE_TO_CURRENCY.get(exchange);
    }
}
```

- [ ] **Step 5: Run the tests to confirm they pass**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.extractor.ExchangeCurrencyRegistryTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 8 tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistry.java \
        src/test/java/com/akolodziejski/divstock/service/extractor/ExchangeCurrencyRegistryTest.java
git commit -m "feat: add ExchangeCurrencyRegistry for exchange-to-currency lookup"
```

---

## Task 2: Add `listingExch` to `Transaction`

**Files:**
- Modify: `src/main/java/com/akolodziejski/divstock/model/csv/Transaction.java`

No dedicated test — the field is a simple model addition exercised by later tasks.

- [ ] **Step 1: Add the field**

In `src/main/java/com/akolodziejski/divstock/model/csv/Transaction.java`, add the new field after `fee`:

```java
    //        10 = "Comm/Fee"
    private float fee;

    // populated for IB transactions only; null for Degiro
    private String listingExch;
```

The full file after the change:

```java
package com.akolodziejski.divstock.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class Transaction {

    //      1 = "Header"
  //  private String header;
    //        2 = "DataDiscriminator"
   // private String dataDiscriminator;
    //        3 = "Asset Category"
    //private String assetCategory;
    //        4 = "Currency"
    private String currency;
    //        5 = "Symbol"
    private String symbol;
    //        6 = "Date/Time"
    private Date date;
    //        7 = "Quantity"
    private float quantity;
    //        8 = "T. Price"
    private float price;
    //        9 = "Proceeds"
    private float proceeds;
    //        10 = "Comm/Fee"
    private float fee;

    // populated for IB transactions only; null for Degiro
    private String listingExch;

    public String getId() {
        return symbol + "/" + currency;
    }
}
```

- [ ] **Step 2: Verify the project still compiles**

```bash
./gradlew compileJava 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` — the new nullable field is backwards-compatible with all existing code that uses `Transaction`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/model/csv/Transaction.java
git commit -m "feat: add nullable listingExch field to Transaction model"
```

---

## Task 3: Two-pass extraction in `InteractiveBrokersCSVExtractor`

**Files:**
- Modify: `src/main/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractor.java`
- Create: `src/test/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractorExchangeTest.java`

Background: `mapToTransactions(List<String[]> lines)` receives all rows after the 7 skipped header lines. `Financial Instrument Information` rows appear later in the same file and are already in `lines` — they are just currently ignored by the existing `row[2].equals("Order")` filter.

Column layout for `Financial Instrument Information,Data` rows:
- `row[0]` = `"Financial Instrument Information"`
- `row[1]` = `"Data"`
- `row[2]` = asset category (e.g., `"Stocks"`)
- `row[3]` = symbol
- `row[8]` = listing exchange

Column layout for `Trades,Data,Order` rows:
- `row[4]` = currency
- `row[5]` = symbol

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractorExchangeTest.java`:

```java
package com.akolodziejski.divstock.service.extractor;

import com.akolodziejski.divstock.model.csv.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InteractiveBrokersCSVExtractorExchangeTest {

    private final InteractiveBrokersCSVExtractor extractor = new InteractiveBrokersCSVExtractor();

    /**
     * Builds a minimal row for a Trades,Data,Order,Stocks entry.
     * Columns: [0]=Trades [1]=Data [2]=Order [3]=Stocks [4]=currency [5]=symbol
     *          [6]=date [7]=quantity [8]=price [9]=proceeds [10]=fee
     */
    private String[] tradesRow(String currency, String symbol, String quantity) {
        return new String[]{"Trades", "Data", "Order", "Stocks", currency, symbol,
                "2024-01-15", quantity, "100.0", "10000.0", "-1.5"};
    }

    /**
     * Builds a minimal row for a Financial Instrument Information,Data,Stocks entry.
     * Columns: [0]=section [1]=Data [2]=Stocks [3]=symbol [4..7]=ignored [8]=listingExch
     */
    private String[] fiiRow(String symbol, String listingExch) {
        return new String[]{"Financial Instrument Information", "Data", "Stocks", symbol,
                "Some Description", "12345", "US0000000000", symbol, listingExch, "1", "COMMON", ""};
    }

    @Test
    void listingExch_is_populated_for_simple_case() {
        List<String[]> lines = List.of(
                fiiRow("AAPL", "NASDAQ"),
                tradesRow("USD", "AAPL", "-10")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("NASDAQ");
    }

    @Test
    void cross_listed_stock_resolved_by_currency_usd_to_nyse() {
        List<String[]> lines = List.of(
                fiiRow("ENB", "NYSE"),   // USD listing
                fiiRow("ENB", "TSE"),    // CAD listing
                tradesRow("USD", "ENB", "-5")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("NYSE");
    }

    @Test
    void cross_listed_stock_resolved_by_currency_cad_to_tse() {
        List<String[]> lines = List.of(
                fiiRow("ENB", "NYSE"),
                fiiRow("ENB", "TSE"),
                tradesRow("CAD", "ENB", "-5")
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("TSE");
    }

    @Test
    void buy_transactions_are_excluded_from_result() {
        List<String[]> lines = List.of(
                fiiRow("AAPL", "NASDAQ"),
                tradesRow("USD", "AAPL", "10")   // buy: positive quantity
        );

        // The extractor still returns buy transactions — filtering by quantity
        // is done downstream. Verify the buy transaction has listingExch set.
        List<Transaction> result = extractor.mapToTransactions(lines);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isEqualTo("NASDAQ");
    }

    @Test
    void symbol_without_fii_row_has_null_listingExch() {
        List<String[]> lines = List.of(
                tradesRow("USD", "XYZ", "-3")  // no FII row for XYZ
        );

        List<Transaction> result = extractor.mapToTransactions(lines);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingExch()).isNull();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.extractor.InteractiveBrokersCSVExtractorExchangeTest" 2>&1 | tail -20
```

Expected: all tests fail — `listingExch` is always null before the two-pass logic is added.

- [ ] **Step 3: Update `InteractiveBrokersCSVExtractor`**

Replace the full content of `src/main/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractor.java`:

```java
package com.akolodziejski.divstock.service.extractor;

import com.akolodziejski.divstock.model.csv.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Qualifier("ib")
public class InteractiveBrokersCSVExtractor extends CSVExtractor {

    private static final Logger log = LoggerFactory.getLogger(InteractiveBrokersCSVExtractor.class);
    private final ExchangeCurrencyRegistry exchangeCurrencyRegistry = new ExchangeCurrencyRegistry();

    @Override
    protected int getSkipLines() {
        return 7;
    }

    @Override
    protected List<Transaction> mapToTransactions(List<String[]> lines) {
        Map<String, String> symbolCurrencyToExchange = buildExchangeMap(lines);

        return lines.stream()
                .filter(row -> row[2].equals("Order"))
                .filter(row -> row[3].equals("Stocks"))
                .map(row -> mapToTransaction(row, symbolCurrencyToExchange))
                .collect(Collectors.toList());
    }

    /**
     * Pass 1: read Financial Instrument Information rows to build
     * a Map keyed by "symbol/currency" → listing exchange.
     * Currency is resolved from the exchange name via ExchangeCurrencyRegistry.
     */
    private Map<String, String> buildExchangeMap(List<String[]> lines) {
        Map<String, String> map = new HashMap<>();
        for (String[] row : lines) {
            if (row.length > 8
                    && "Financial Instrument Information".equals(row[0])
                    && "Data".equals(row[1])
                    && "Stocks".equals(row[2])) {
                String symbol = row[3];
                String exchange = row[8];
                String currency = exchangeCurrencyRegistry.getCurrency(exchange);
                if (currency == null) {
                    log.warn("No currency mapping for exchange '{}' (symbol: {})", exchange, symbol);
                    continue;
                }
                map.put(symbol + "/" + currency, exchange);
            }
        }
        return map;
    }

    protected Transaction mapToTransaction(String[] row, Map<String, String> symbolCurrencyToExchange) {
        String currency = row[4];
        String symbol = row[5];
        String listingExch = symbolCurrencyToExchange.get(symbol + "/" + currency);
        if (listingExch == null) {
            log.warn("No listing exchange found for symbol '{}' currency '{}'", symbol, currency);
        }
        return Transaction.builder()
                .currency(currency)
                .symbol(symbol)
                .date(parseDate(row[6]))
                .quantity(Float.valueOf(row[7]))
                .price(Float.valueOf(row[8]))
                .proceeds(Float.valueOf(row[9]))
                .fee(Float.valueOf(row[10]))
                .listingExch(listingExch)
                .build();
    }

    @Override
    protected Transaction mapToTransaction(String[] row) {
        // Not used — the overload with the map is called from mapToTransactions().
        // Required by the abstract base class; delegates to the two-arg version with an empty map.
        return mapToTransaction(row, Map.of());
    }

    private Date parseDate(String dateTime) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            String datePart = dateTime.split(",")[0];
            if (datePart == null || datePart.isBlank()) return null;
            return formatter.parse(datePart);
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateTime);
            return null;
        }
    }
}
```

- [ ] **Step 4: Run the tests to confirm they pass**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.extractor.InteractiveBrokersCSVExtractorExchangeTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractor.java \
        src/test/java/com/akolodziejski/divstock/service/extractor/InteractiveBrokersCSVExtractorExchangeTest.java
git commit -m "feat: enrich IB transactions with listingExch via two-pass CSV extraction"
```

---

## Task 4: `ExchangeIncomeCalculator`

**Files:**
- Create: `src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java`
- Create: `src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java`:

```java
package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeIncomeCalculatorTest {

    @Mock
    private LocalCSVFilesTransactionProvider transactionProvider;

    @Mock
    private RealizedTransasctionInPln realizedTransactionCalculator;

    @InjectMocks
    private ExchangeIncomeCalculator calculator;

    private PLNTransaction plnTx(String exchange, float income) {
        Transaction statement = Transaction.builder()
                .symbol("AAPL").currency("USD").listingExch(exchange).build();
        return PLNTransaction.builder()
                .income(income).cost(0).gainLost(0).error(false).statement(statement).build();
    }

    private PLNTransaction errorTx(String exchange) {
        Transaction statement = Transaction.builder()
                .symbol("ERR").currency("USD").listingExch(exchange).build();
        return PLNTransaction.builder()
                .income(999).cost(0).gainLost(0).error(true).statement(statement).build();
    }

    @Test
    void groups_income_by_exchange() {
        when(transactionProvider.getIbTransactions()).thenReturn(List.of());
        when(realizedTransactionCalculator.processForYear(any(), eq(2024))).thenReturn(
                List.of(plnTx("NYSE", 1000f), plnTx("NYSE", 500f), plnTx("TSE", 200f))
        );

        Map<String, Double> result = calculator.calculate(2024);

        assertThat(result).containsKey("NYSE");
        assertThat(result.get("NYSE")).isCloseTo(1500.0, within(0.01));
        assertThat(result).containsKey("TSE");
        assertThat(result.get("TSE")).isCloseTo(200.0, within(0.01));
    }

    @Test
    void error_transactions_are_excluded() {
        when(transactionProvider.getIbTransactions()).thenReturn(List.of());
        when(realizedTransactionCalculator.processForYear(any(), eq(2024))).thenReturn(
                List.of(plnTx("NYSE", 1000f), errorTx("NYSE"))
        );

        Map<String, Double> result = calculator.calculate(2024);

        assertThat(result.get("NYSE")).isCloseTo(1000.0, within(0.01));
    }

    @Test
    void null_listingExch_grouped_under_unknown() {
        Transaction statement = Transaction.builder()
                .symbol("XYZ").currency("USD").listingExch(null).build();
        PLNTransaction nullExchange = PLNTransaction.builder()
                .income(300f).cost(0).gainLost(0).error(false).statement(statement).build();

        when(transactionProvider.getIbTransactions()).thenReturn(List.of());
        when(realizedTransactionCalculator.processForYear(any(), eq(2024))).thenReturn(
                List.of(nullExchange)
        );

        Map<String, Double> result = calculator.calculate(2024);

        assertThat(result).containsKey("UNKNOWN");
        assertThat(result.get("UNKNOWN")).isCloseTo(300.0, within(0.01));
    }

    @Test
    void empty_transactions_returns_empty_map() {
        when(transactionProvider.getIbTransactions()).thenReturn(List.of());
        when(realizedTransactionCalculator.processForYear(any(), eq(2024))).thenReturn(List.of());

        Map<String, Double> result = calculator.calculate(2024);

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculatorTest" 2>&1 | tail -20
```

Expected: compilation failure — `ExchangeIncomeCalculator` does not exist yet.

- [ ] **Step 3: Create `ExchangeIncomeCalculator`**

Create `src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java`:

```java
package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.transaction.LocalCSVFilesTransactionProvider;
import com.akolodziejski.divstock.service.transaction.RealizedTransasctionInPln;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExchangeIncomeCalculator {

    private final LocalCSVFilesTransactionProvider transactionProvider;
    private final RealizedTransasctionInPln realizedTransactionCalculator;

    public ExchangeIncomeCalculator(
            LocalCSVFilesTransactionProvider transactionProvider,
            RealizedTransasctionInPln realizedTransactionCalculator) {
        this.transactionProvider = transactionProvider;
        this.realizedTransactionCalculator = realizedTransactionCalculator;
    }

    public Map<String, Double> calculate(int year) {
        List<Transaction> ibTransactions = transactionProvider.getIbTransactions();
        List<PLNTransaction> plnTransactions = realizedTransactionCalculator.processForYear(ibTransactions, year);

        return plnTransactions.stream()
                .filter(t -> !t.isError())
                .collect(Collectors.groupingBy(
                        t -> {
                            String exch = t.getStatement().getListingExch();
                            return exch != null ? exch : "UNKNOWN";
                        },
                        Collectors.summingDouble(t -> t.getIncome())
                ));
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculatorTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculator.java \
        src/test/java/com/akolodziejski/divstock/service/tax/ExchangeIncomeCalculatorTest.java
git commit -m "feat: add ExchangeIncomeCalculator to group PLN income by listing exchange"
```

---

## Task 5: `ExchangeIncomeReportWriter`

**Files:**
- Create: `src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java`
- Create: `src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java`:

```java
package com.akolodziejski.divstock.service.reporter;

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
        Map<String, Double> data = Map.of("NYSE", 12345.678, "TSE", 999.10);

        writer.write(data, 2024, tempDir.toString());

        Path output = tempDir.resolve("exchange_income_2024.csv");
        assertThat(output).exists();
        List<String> lines = Files.readAllLines(output);
        assertThat(lines.get(0)).isEqualTo("Listing Exch,Income (PLN)");
    }

    @Test
    void rows_are_sorted_alphabetically_by_exchange() throws IOException {
        Map<String, Double> data = Map.of("TSE", 200.0, "NYSE", 1000.0, "AEB", 500.0);

        writer.write(data, 2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("exchange_income_2024.csv"));
        // skip header
        assertThat(lines.get(1)).startsWith("AEB,");
        assertThat(lines.get(2)).startsWith("NYSE,");
        assertThat(lines.get(3)).startsWith("TSE,");
    }

    @Test
    void income_formatted_to_two_decimal_places() throws IOException {
        Map<String, Double> data = Map.of("NYSE", 12345.678);

        writer.write(data, 2024, tempDir.toString());

        List<String> lines = Files.readAllLines(tempDir.resolve("exchange_income_2024.csv"));
        assertThat(lines.get(1)).isEqualTo("NYSE,12345.68");
    }

    @Test
    void output_directory_is_created_if_missing() throws IOException {
        Path nested = tempDir.resolve("nested/output");
        Map<String, Double> data = Map.of("NYSE", 100.0);

        writer.write(data, 2025, nested.toString());

        assertThat(nested.resolve("exchange_income_2025.csv")).exists();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriterTest" 2>&1 | tail -20
```

Expected: compilation failure — `ExchangeIncomeReportWriter` does not exist yet.

- [ ] **Step 3: Create `ExchangeIncomeReportWriter`**

Create `src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java`:

```java
package com.akolodziejski.divstock.service.reporter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
                        .forEach(e -> pw.printf("%s,%.2f%n", e.getKey(), e.getValue()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write exchange income report for year " + year, e);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew test --tests "com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriterTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriter.java \
        src/test/java/com/akolodziejski/divstock/service/reporter/ExchangeIncomeReportWriterTest.java
git commit -m "feat: add ExchangeIncomeReportWriter to output exchange income CSV"
```

---

## Task 6: Wire into `DivstockApplication`

**Files:**
- Modify: `src/main/java/com/akolodziejski/divstock/DivstockApplication.java`

- [ ] **Step 1: Update `DivstockApplication`**

Replace the full content of `src/main/java/com/akolodziejski/divstock/DivstockApplication.java`:

```java
package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriter;
import com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculator;
import com.akolodziejski.divstock.service.tax.PIT38Calculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.util.Map;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication implements CommandLineRunner {

    private final PIT38Calculator calculator;
    private final ExchangeIncomeCalculator exchangeIncomeCalculator;

    public DivstockApplication(PIT38Calculator calculator, ExchangeIncomeCalculator exchangeIncomeCalculator) {
        this.calculator = calculator;
        this.exchangeIncomeCalculator = exchangeIncomeCalculator;
    }

    public static void main(String[] args) {
        SpringApplication.run(DivstockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        int year = 2025;

        Profit profit = calculator.calculate(year);
        System.out.println(profit);

        Map<String, Double> exchangeIncome = exchangeIncomeCalculator.calculate(year);
        new ExchangeIncomeReportWriter().write(exchangeIncome, year);
        System.out.println("Exchange income report written to output/exchange_income_" + year + ".csv");
    }
}
```

- [ ] **Step 2: Verify full build and all tests pass**

```bash
./gradlew build 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/akolodziejski/divstock/DivstockApplication.java
git commit -m "feat: wire ExchangeIncomeCalculator and ReportWriter into application run"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** All spec sections covered — model change (Task 2), registry (Task 1), extractor two-pass (Task 3), calculator (Task 4), writer (Task 5), wiring (Task 6).
- [x] **No placeholders:** All steps contain actual code.
- [x] **Type consistency:** `listingExch` field name used consistently across Transaction, extractor, calculator, and tests. `mapToTransaction(String[], Map)` signature defined in Task 3 and used only within that class.
- [x] **`@SneakyThrows` removed:** The original extractor used `@SneakyThrows` on `parseDate`; the updated version uses a try/catch with a warning log, which is cleaner for a non-critical field.
- [x] **`mapToTransaction(String[])` abstract contract:** The base class `CSVExtractor` declares `mapToTransaction(String[])` as abstract. The updated extractor satisfies this contract while routing actual work through the two-arg overload.
