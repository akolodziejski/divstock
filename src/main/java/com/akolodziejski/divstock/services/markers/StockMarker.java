package com.akolodziejski.divstock.services.markers;

public interface StockMarker {
    Double markStock(String ticker);
    String name();
}
