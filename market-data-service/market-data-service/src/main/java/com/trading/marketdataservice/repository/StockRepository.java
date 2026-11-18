package com.trading.marketdataservice.repository;

import com.trading.marketdataservice.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock,String> {
}
