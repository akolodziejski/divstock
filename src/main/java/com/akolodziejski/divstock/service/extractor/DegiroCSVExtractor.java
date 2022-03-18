package com.akolodziejski.divstock.service.extractor;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@Qualifier("degiro")
public class DegiroCSVExtractor  extends  CSVExtractor{

    protected Transaction mapToTransaction(String[] row) {

        return  Transaction.builder()
                //      1 = "Header"
               // .header( row[1])
                //        2 = "DataDiscriminator"
                //.dataDiscriminator(row[2])
                //        3 = "Asset Category"
               // .assetCategory(row[3])
                //        4 = "Currency"
                .currency(row[8])
                //        5 = "Symbol"
                .symbol(row[2])
                //        6 = "Date/Time"
                .date(parseDate(row[0]))
                //        7 = "Quantity"
                .quantity(Float.valueOf(row[6]))
                //        8 = "T. Price"
                .price(Float.valueOf(row[7])).build();
                //        9 = "Proceeds"
                //.proceeds(Float.valueOf(row[9]))
                //        10 = "Comm/Fee"
                //.fee(Float.valueOf(row[10])).build();
    }

    @SneakyThrows
    private Date parseDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        return formatter.parse(date);
    }
}
