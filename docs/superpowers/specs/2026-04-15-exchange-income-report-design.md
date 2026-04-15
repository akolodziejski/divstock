# Exchange Income Report — Design Spec
**Date:** 2026-04-15

## Goal

Group IB sell transactions by the listing exchange of the sold stock and produce a CSV report showing the total PLN income per exchange for a given year.

Input: CSV files in `statements/ib/trans/` (Degiro is excluded).
Output: `output/exchange_income_<year>.csv` with columns `Listing Exch,Income (PLN)`.

---

## Architecture

Four components are added or modified:

1. `Transaction` model — add `listingExch` field
2. `ExchangeCurrencyRegistry` — hardcoded exchange→currency lookup
3. `InteractiveBrokersCSVExtractor` — enrich transactions with `listingExch`
4. `ExchangeIncomeCalculator` — new service, groups PLN income by exchange
5. `ExchangeIncomeReportWriter` — CSV output utility
6. `DivstockApplication` — wire the new report call

---

## Section 1 — Data model & exchange mapping

### `Transaction` (model/csv/Transaction.java)

Add one nullable field:

```java
private String listingExch;
```

Degiro transactions leave this null. All IB stock transactions will have it populated.

### `ExchangeCurrencyRegistry` (service/extractor/ExchangeCurrencyRegistry.java)

Plain class (no Spring annotation). Holds a hardcoded `Map<String, String>` mapping listing exchange → primary trading currency. Used by `InteractiveBrokersCSVExtractor` to resolve which currency a given exchange listing trades in, enabling disambiguation of cross-listed symbols.

Initial entries:

| Exchange | Currency |
|----------|----------|
| NYSE     | USD      |
| NASDAQ   | USD      |
| AMEX     | USD      |
| TSE      | CAD      |
| LSE      | GBP      |
| SEHK     | HKD      |
| SGX      | SGD      |
| AEB      | EUR      |

Extensible: new entries can be added as new exchanges appear in IB statements.

### `InteractiveBrokersCSVExtractor` — updated `mapToTransactions()`

The method receives the full `List<String[]>` of all rows after the skipped header lines. It currently filters to `Trades,Data,Order,Stocks` rows in a single pass.

Updated to do two passes over the same list:

**Pass 1** — collect `Financial Instrument Information,Data,Stocks` rows:
- Column layout: `[0]=section, [1]=Data, [2]=AssetCategory, [3]=Symbol, [4]=Description, [5]=Conid, [6]=SecurityID, [7]=Underlying, [8]=ListingExch, ...`
- For each such row, resolve `currency = ExchangeCurrencyRegistry.getCurrency(row[8])`
- Build `Map<String, String> symbolCurrencyToExchange` keyed by `symbol + "/" + currency`

**Pass 2** — existing Trades parsing, now also calling `transaction.setListingExch(symbolCurrencyToExchange.get(symbol + "/" + currency))` for each built Transaction.

If a symbol+currency key is not found in the map (e.g. an exchange not yet in the registry), `listingExch` is left null and a warning is logged.

---

## Section 2 — `ExchangeIncomeCalculator`

**Package:** `service/tax`
**Annotation:** `@Service`

**Dependencies (constructor-injected):**
- `LocalCSVFilesTransactionProvider`
- `RealizedTransasctionInPln`

**Public method:**

```java
public Map<String, Double> calculate(int year)
```

**Logic:**
1. `List<Transaction> ibTrans = transactionProvider.getIbTransactions()`
2. `List<PLNTransaction> plnTrans = realizedTransactionCalculator.processForYear(ibTrans, year)`
3. Filter out error transactions (`!t.isError()`)
4. Group by `t.getStatement().getListingExch()`, defaulting to `"UNKNOWN"` if null
5. Sum `t.getIncome()` per group
6. Return `Map<String, Double>`

Reuses the full FIFO + NBP PLN conversion pipeline from `RealizedTransasctionInPln` without any changes to that class.

---

## Section 3 — `ExchangeIncomeReportWriter`

**Package:** `service/reporter`
**Type:** Plain class (no Spring annotation)

**Method:**

```java
public void write(Map<String, Double> exchangeIncomeMap, int year)
```

**Behaviour:**
- Output path: `output/exchange_income_<year>.csv`
- Creates `output/` directory if it does not exist
- Writes header: `Listing Exch,Income (PLN)`
- Writes one row per exchange, sorted alphabetically by exchange name
- Income values formatted to 2 decimal places

**Example output:**
```
Listing Exch,Income (PLN)
AEB,8234.12
LSE,3421.00
NASDAQ,45678.90
NYSE,123456.78
TSE,9876.54
```

---

## Section 4 — Wiring in `DivstockApplication`

`DivstockApplication.run()` is updated to also:
1. Inject `ExchangeIncomeCalculator` as a constructor parameter (Spring bean)
2. Instantiate `ExchangeIncomeReportWriter` directly (`new ExchangeIncomeReportWriter()`) — it has no dependencies
3. Call `exchangeIncomeCalculator.calculate(year)` for the configured year
4. Pass the result to `exchangeIncomeReportWriter.write(map, year)`

The existing `PIT38Calculator` call is left unchanged.

---

## Error handling

- Symbol+currency not found in `ExchangeCurrencyRegistry`: log a warning, set `listingExch = null`, group under `"UNKNOWN"` in the output.
- `output/` directory creation failure: propagate as a runtime exception with a clear message.

---

## Testing

- Unit test for `ExchangeCurrencyRegistry`: verify known exchange→currency mappings.
- Unit test for the two-pass logic in `InteractiveBrokersCSVExtractor`: given a small synthetic CSV with both `Trades` and `Financial Instrument Information` rows, verify transactions have correct `listingExch` set, including the cross-listed case (e.g. ENB/USD → NYSE, ENB/CAD → TSE).
- Unit test for `ExchangeIncomeCalculator.calculate()`: mock `LocalCSVFilesTransactionProvider` and `RealizedTransasctionInPln`, verify grouping and summing logic.
- Integration/snapshot test for `ExchangeIncomeReportWriter`: verify CSV output format and alphabetical sort.
