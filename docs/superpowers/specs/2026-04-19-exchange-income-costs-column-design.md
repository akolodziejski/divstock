# Design: Add Costs (PLN) Column to Exchange Income Report

**Date:** 2026-04-19

## Goal

Extend the exchange income CSV report with a `Costs (PLN)` column — the aggregated PLN cost basis per listing exchange for the given year.

**Current output:**
```
Listing Exch,Income (PLN)
LSE,5635.77
```

**Target output:**
```
Listing Exch,Income (PLN),Costs (PLN)
LSE,5635.77,4800.00
```

## Data Source

`PLNTransaction.cost` (already populated by `RealizedTransasctionInPln`) holds the PLN cost basis for each sell transaction. No new calculation is needed — only aggregation.

## Components

### 1. New record: `ExchangeIncomeSummary`

**Location:** `src/main/java/com/akolodziejski/divstock/model/reporter/ExchangeIncomeSummary.java`

```java
public record ExchangeIncomeSummary(double income, double cost) {}
```

Named fields replace the fragile `Map<String, Double>` single-value approach.

### 2. `ExchangeIncomeCalculator`

- Return type: `Map<String, Double>` → `Map<String, ExchangeIncomeSummary>`
- Replace `Collectors.groupingBy` + `summingDouble` with a `toMap` or `groupingBy` + custom collector that sums both `income` and `cost` per `listingExch`.
- Error-transaction filter unchanged.

### 3. `ExchangeIncomeReportWriter`

- Method signature: `Map<String, Double>` → `Map<String, ExchangeIncomeSummary>`
- Header: `Listing Exch,Income (PLN),Costs (PLN)`
- Row format: `%s,%.2f,%.2f` — exch, income, cost
- Sort order (by key) unchanged.

### 4. `DivstockApplication`

No structural change. `exchangeIncomeCalculator.calculate(year)` now returns `Map<String, ExchangeIncomeSummary>`; the writer call signature is updated to match.

## Error handling

No new error cases. Transactions with `error=true` remain filtered before aggregation. Null `listingExch` falls back to `"UNKNOWN"` (existing behaviour).

## Testing

- Unit test `ExchangeIncomeCalculator`: verify that both income and cost are summed correctly per exchange, and that error transactions are excluded.
- Unit test `ExchangeIncomeReportWriter`: verify CSV output contains three columns with correct values.
