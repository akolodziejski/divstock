package com.akolodziejski.divstock;

import com.akolodziejski.divstock.services.markers.PayoutRatioMarker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@SpringBootTest
class DivstockApplicationTests {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private PayoutRatioMarker payoutRatioMarker;

	@Test
	void contextLoads() {
		DividendHistoryModel str = restTemplate.getForObject(
				"https://financialmodelingprep.com/api/v3/historical-price-full/stock_dividend/MCD",
				DividendHistoryModel.class);
	}

	@Test
	void markByPayoutRatioTest() {
		Double markerResoult = payoutRatioMarker.markStock("MCD");
	}

}

class DividendHistoryModel {
	private String symbol;
	private List<Dividend> historical;

	public DividendHistoryModel() {
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public List<Dividend> getHistorical() {
		return historical;
	}

	public void setHistorical(List<Dividend> historical) {
		this.historical = historical;
	}
}

class Dividend {
	private Date date;
	private Double adjDividend;

	public Dividend() {
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Double getAdjDividend() {
		return adjDividend;
	}

	public void setAdjDividend(Double adjDividend) {
		this.adjDividend = adjDividend;
	}
}
