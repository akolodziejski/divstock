package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.service.CurrentStateReporter;
import com.akolodziejski.divstock.service.TransactionsCSVExtractor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.util.*;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication  implements CommandLineRunner {

	private final TransactionsCSVExtractor transactionsCSVExtractor;
	private final CurrentStateReporter currentStateReporter;

	public DivstockApplication(TransactionsCSVExtractor transactionsCSVExtractor, CurrentStateReporter currentStateReporter) {
		this.transactionsCSVExtractor = transactionsCSVExtractor;
		this.currentStateReporter = currentStateReporter;
	}

	public static void main(String[] args) {
		SpringApplication.run(DivstockApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		List<Transaction> allTransactions = transactionsCSVExtractor.getTransactions(Arrays.asList(args));
		currentStateReporter.process(allTransactions);
	}

}
