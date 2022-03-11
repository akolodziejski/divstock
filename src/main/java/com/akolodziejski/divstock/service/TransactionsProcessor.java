package com.akolodziejski.divstock.service;

import com.akolodziejski.divstock.model.csv.Transaction;

import java.util.List;

public interface TransactionsProcessor {

    void process(List<Transaction> transactions);
}
