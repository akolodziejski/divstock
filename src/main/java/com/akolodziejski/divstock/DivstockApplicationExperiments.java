package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.reporter.DivGrowthChecker;
import com.akolodziejski.divstock.service.tax.PIT38Calculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplicationExperiments implements CommandLineRunner {

	private DivGrowthChecker calculator;

	public DivstockApplicationExperiments(DivGrowthChecker calculator) {
		this.calculator = calculator;

	}

	public static void main(String[] args) {
		SpringApplication.run(DivstockApplicationExperiments.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		calculator.stockDetails("PEP");
		calculator.disctinctSymbols();
	}

}
