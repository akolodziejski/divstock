package com.akolodziejski.divstock.model.api;

import lombok.Data;

import java.util.List;

@Data
public class NBPCurrencyRate {
    private List<NBPRate> rates;
}
