package com.akolodziejski.divstock.model.reporter;

import com.akolodziejski.divstock.model.csv.Transaction;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class PLNTransaction {
    private float gainLost; // TODO remove
    private float cost;
    private float income;
    private float costInCurrency;         // cost basis in original transaction currency
    private float gainLostInCurrency;     // gain/loss in original transaction currency
    private double taxPaid;
    private boolean error;
    private Transaction statement;
}
