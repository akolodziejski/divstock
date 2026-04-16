package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.tax.Profit;
import com.akolodziejski.divstock.service.reporter.ExchangeIncomeReportWriter;
import com.akolodziejski.divstock.service.tax.ExchangeIncomeCalculator;
import com.akolodziejski.divstock.service.tax.PIT38Calculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.util.Map;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DivstockApplication implements CommandLineRunner {

    private final PIT38Calculator calculator;
    private final ExchangeIncomeCalculator exchangeIncomeCalculator;

    public DivstockApplication(PIT38Calculator calculator, ExchangeIncomeCalculator exchangeIncomeCalculator) {
        this.calculator = calculator;
        this.exchangeIncomeCalculator = exchangeIncomeCalculator;
    }

    public static void main(String[] args) {
        SpringApplication.run(DivstockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        int year = 2025;

        Profit profit = calculator.calculate(year);
        System.out.println(profit);

        Map<String, Double> exchangeIncome = exchangeIncomeCalculator.calculate(year);
        new ExchangeIncomeReportWriter().write(exchangeIncome, year);
        System.out.println("Exchange income report written to output/exchange_income_" + year + ".csv");
    }
}
