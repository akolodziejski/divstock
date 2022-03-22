package com.akolodziejski.divstock.model.tax;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Profit {
    private double income;
    private double cost;
    private double taxPaid;
}
