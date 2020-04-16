package com.akolodziejski.divstock;

import com.akolodziejski.divstock.model.Rank;
import com.akolodziejski.divstock.services.RankService;
import com.akolodziejski.divstock.services.TickersService;
import com.akolodziejski.divstock.services.markers.PayoutRatioMarker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
class DivstockApplicationTests {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private RankService rankService;

	@Autowired
	private TickersService tickersService;

	@Test
	void contextLoads() {
		DividendHistoryModel str = restTemplate.getForObject(
				"https://financialmodelingprep.com/api/v3/historical-price-full/stock_dividend/MCD",
				DividendHistoryModel.class);
	}

	@Test
	void markAllSp500() {
		var tickers = listOf(tickersService.getTickers().stream().limit(10));
		var ranks = rankService.generateRanks(tickers);

	}

	private Comparator<Rank> getRankComparator() {
		return (f, s) -> (int) (f.getResult() - s.getResult());
	}

	private <T> List<T> listOf(Stream<T> stream) {
		return stream.collect(Collectors.toList());
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
