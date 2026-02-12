package com.akolodziejski.divstock.service.transaction;

import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatementsFileService {

    public static final String STATEMENTS_DEGIRO_TRANS = "statements/degiro/trans";
    public static final String STATEMENTS_DEGIRO_DIV = "statements/degiro/div";
    public static final String STATEMENTS_IB_TRANS = "statements/ib/trans";
    public static final String STATEMENTS_IB_DIV = "statements/ib/div";

    List<String> getDegiroTransactions() {
        return getFilesInFolder(STATEMENTS_DEGIRO_TRANS);
    }

    List<String> getDegiroDividends() {
        return getFilesInFolder(STATEMENTS_DEGIRO_DIV);
    }

    List<String> getIbTransactions() {
        return getFilesInFolder(STATEMENTS_IB_TRANS);
    }

    List<String> getIbDividends() {
        return getFilesInFolder(STATEMENTS_IB_DIV);
    }

    private static List<String> getFilesInFolder(String folderPath) {
        return Arrays.asList(new File(folderPath).list()).stream()
                .map( fName -> folderPath + "/" + fName).collect(Collectors.toList());
    }
}
