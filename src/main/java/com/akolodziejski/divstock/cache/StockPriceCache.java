package com.akolodziejski.divstock.cache;

import com.akolodziejski.divstock.service.Common;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;


@Service
public class StockPriceCache {

    private DB stockPriceDB;
    private Map<String, Float> price_at_date;

    @PostConstruct
    private void init() {
        stockPriceDB = DBMaker.fileDB("stock_prices.db")
                .closeOnJvmShutdown()
                .make();
        price_at_date = stockPriceDB.hashMap("price_at_date", Serializer.STRING, Serializer.FLOAT).createOrOpen();
    }

    public Float get(String currency, Date date) {
        return price_at_date.get(getCurrencyDateKey(currency, date));
    }

    public void put(String currency, Date date, float price) {
        price_at_date.put(getCurrencyDateKey(currency, date), price);
        stockPriceDB.commit(); // TODO need to check if necessarry
    }

    @NotNull
    private String getCurrencyDateKey(String currency, Date date) {
        return currency.toLowerCase() + "/" + Common.SIMPLE_FORMAT.format(date);
    }


    public void store() {
        stockPriceDB.commit();
    }
}
