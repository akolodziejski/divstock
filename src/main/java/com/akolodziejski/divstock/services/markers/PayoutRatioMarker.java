package com.akolodziejski.divstock.services.markers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayoutRatioMarker implements StockMarker {

    @Autowired
    RestTemplate restTemplate;

    @Override
    public Double markStock(String ticker) {

        CompanyKeyMetrics companyMetrics
                = restTemplate.getForObject("https://financialmodelingprep.com/api/v3/company-key-metrics/" + ticker, CompanyKeyMetrics.class);
        return null;
    }

    @Override
    public String name() {
        return "PayoutRatio Marker";
    }
}
class CompanyMetric {
    @JsonProperty("Payout Ratio")
    private Double payoutRatio;
    public CompanyMetric() {
    }

    public Double getPayoutRatio() {
        return payoutRatio;
    }

    public void setPayoutRatio(Double payoutRatio) {
        this.payoutRatio = payoutRatio;
    }
}

class CompanyKeyMetrics {

    private List<CompanyMetric> metrics;

    public CompanyKeyMetrics() {
    }

    public List<CompanyMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<CompanyMetric> metrics) {
        this.metrics = metrics;
    }
}
