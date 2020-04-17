package com.akolodziejski.divstock.services.markers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PayoutRatioMarker implements StockMarker {

    private static  final String SOURCE_URL = "https://financialmodelingprep.com/api/v3/company-key-metrics/";

    @Autowired
    RestTemplate restTemplate;

    @Override
    @Cacheable("rank")
    public Double markStock(String ticker) {

        var metrics = getMetricsWithExternalApi(ticker);

        if(metrics == null || metrics.isEmpty()) {
            return 0.0;
        }

        CompanyMetric lastMetric = metrics.stream().findFirst().orElseThrow();
        Double payoutRatioFractional = lastMetric.getPayoutRatio();

        if(payoutRatioFractional == null || payoutRatioFractional <= 0.0)
            return 0.0;

        Double payoutRatio = payoutRatioFractional * 100.0;

        log.info("PayoutRatio marker for {} is {}.", ticker, payoutRatio);
        return 100 - payoutRatio;
    }


    private List<CompanyMetric> getMetricsWithExternalApi(String ticker) {
        return restTemplate.getForObject( SOURCE_URL + ticker, CompanyKeyMetrics.class).getMetrics();
    }

    @Override
    public String name() {
        return "PayoutRatio Marker";
    }
}

@Data
class CompanyMetric {
    @JsonProperty("Payout Ratio")
    private Double payoutRatio;
}

@Data
class CompanyKeyMetrics {
    private List<CompanyMetric> metrics;
}
