package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.csv.Transaction;
import com.akolodziejski.divstock.model.reporter.PLNTransaction;
import com.akolodziejski.divstock.service.extractor.CSVExtractor;
import com.akolodziejski.divstock.service.extractor.InteractiveBrokersCSVExtractor;
import com.akolodziejski.divstock.service.reporter.CurrentStateReporter;

import com.akolodziejski.divstock.service.reporter.RealizedTransasctionInPln;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.util.*;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication  implements CommandLineRunner {

	private final CSVExtractor transactionsCSVExtractor;
	private final RealizedTransasctionInPln currentStateReporter;

	public DivstockApplication(@Qualifier("degiro") CSVExtractor transactionsCSVExtractor, RealizedTransasctionInPln currentStateReporter) {
		this.transactionsCSVExtractor = transactionsCSVExtractor;
		this.currentStateReporter = currentStateReporter;
	}

	public static void main(String[] args) {
		SpringApplication.run(DivstockApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		List<Transaction> allTransactions = transactionsCSVExtractor.getTransactions(Arrays.asList(args));
		List<PLNTransaction> plnTransactions = currentStateReporter.processForYear(allTransactions, 2021);



		double sum = plnTransactions.stream().mapToDouble(PLNTransaction::getGainLost).sum();
	}

}
