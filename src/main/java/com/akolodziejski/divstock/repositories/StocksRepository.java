package com.akolodziejski.divstock.repositories;

import com.akolodziejski.divstock.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StocksRepository extends JpaRepository<Stock, Long> {
}
