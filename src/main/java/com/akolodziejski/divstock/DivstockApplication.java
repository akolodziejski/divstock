package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.reporter.DivGrowthReporter;
import com.akolodziejski.divstock.service.tax.OptionsCalculator;
import com.akolodziejski.divstock.service.tax.PIT38Calculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication  implements CommandLineRunner {

	private PIT38Calculator calculator;

	public DivstockApplication(PIT38Calculator calculator) {
		this.calculator = calculator;

	}

	public static void main(String[] args) {
		SpringApplication.run(DivstockApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Profit profit = calculator.calculate(2023);
		System.out.println(profit);
	}

}
