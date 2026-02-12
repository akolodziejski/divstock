package com.akolodziejski.divstock.service.tax;

import com.akolodziejski.divstock.model.tax.Profit;
import org.springframework.stereotype.Service;

@Service
public class PIT38Calculator {

    private final static double PL_INCOME_TAX = 0.19;

    private OptionsCalculator optionsCalculator;
    private DividendsCalculator dividendsCalculator;
    private RealizedTransactionsCalculator realizedCalculator;

    public PIT38Calculator(OptionsCalculator optionsCalculator, DividendsCalculator dividendsCalculator, RealizedTransactionsCalculator realizedCalculator) {
        this.optionsCalculator = optionsCalculator;
        this.dividendsCalculator = dividendsCalculator;
        this.realizedCalculator = realizedCalculator;
    }


    public Profit calculate(int year) {
        Profit dividendProfits = dividendsCalculator.calculate(year);
        Profit sellsProfits = realizedCalculator.calculate(year);
        Profit optionsProfits = optionsCalculator.calculate(year);

        //TODO PIT-ZG - only income distribution from sells
//        if(year > 2021) {
//            throw new RuntimeException("Check sells if need to PIT-ZG distribution");
//        }

//        double divTaxForPL = calculatePLTax(dividendProfits);
//        double sellsTaxForPL = calculatePLTax(sellsProfits);

        return Profit.builder()
                .income(optionsProfits.getIncome() + dividendProfits.getIncome() + sellsProfits.getIncome())
                .cost(optionsProfits.getCost() + dividendProfits.getCost() + sellsProfits.getCost())
                .taxPaid(dividendProfits.getTaxPaid() + sellsProfits.getTaxPaid())
                .build();
    }

//    private double calculatePLTax(Profit profit) {
//
//        double plExpectedTax = (profit.getIncome() - profit.getCost()) * PL_INCOME_TAX;
//        return  plExpectedTax - profit.getTaxPaid();
//    }
}
