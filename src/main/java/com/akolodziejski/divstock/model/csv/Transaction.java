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

    public String getId() {
        return symbol + "/" + currency;
    }
}
