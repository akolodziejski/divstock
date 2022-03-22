package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.tax.PIT38TaxCalculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication  implements CommandLineRunner {


	private PIT38TaxCalculator calculator;

	public DivstockApplication(PIT38TaxCalculator calculator) {
		this.calculator = calculator;
	}

	public static void main(String[] args) {
		SpringApplication.run(DivstockApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Profit profit = calculator.calculate(2021);

	}

}
