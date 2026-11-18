package com.trading.marketdataservice.service;


import com.trading.marketdataservice.dto.StockPrice;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final StockRepository  stockRepository;
    private final Random random =new Random();

    private final Map<String, StockPrice> stockPrices=
            new ConcurrentHashMap<>();


    @PostConstruct
    public void initializePrices(){
        log.info("Loading stock Data....");

        List<Stock> stocks=stockRepository.findByActiveTrue();

        if(stocks.isEmpty()){
            log.warn("No active stocks found!");
            return;
        }

        stocks.forEach(Stock stock -> {
            StockPrice stockPrice =new StockPrice();
            stockPrice.setSymbol(stock.getSymbol());
            stockPrice.setCompanyName(stock.getCompanyName());
            stockPrice.setPrice(stock.getInitialPrice());
            stockPrice.setOpen(stock.getInitialPrice());
            stockPrice.setHigh(stock.getInitialPrice());
            stockPrice.setLow(stock.getInitialPrice());
            stockPrice.setChange(BigDecimal.ZERO);
            stockPrice.setChangePercent(BigDecimal.ZERO);
            stockPrice.setVolume(0L);
            stockPrice.setTimestamp(LocalDateTime.now());

            stockPrice.put(stock.getSymbol(),stockPrice);

            log.info("Loaded Stocks: {} at {}",stock.getSymbol(),stock.getInitialPrice());
        });

        log.info("Initialized {} stocks",stockPrices.size());
    }



    @Scheduled(fixedRateString = "${market.price-update-interval}")
    public void updatePrices(){

        if(stockPrices.isEmpty()){
            log.warn("No stocks loaded");
            return;
        }

        stockPrices.forEach(( String symbol,StockPrice currentPrice) -> {
            BigDecimal oldPrice =currentPrice.getPrice();

            double priceMovement = (random.nextDouble() - 0.5) * 0.5;
            BigDecimal priceChange = oldPrice.multiply(BigDecimal.valueOf(
                    changePercent /100)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal newPrice=oldPrice.add(priceChange).setScale(2,RoundingMode.HALF_UP);

            if(newPrice.compareTo(BigDecimal.valueOf(1))<0){
                newPrice = BigDecimal.valueOf(1);
            }

            BigDecimal changeAmount = newPrice.subtract(currentPrice.getOpen())
                    .setScale(1,RoundingMode.HALF_UP);

            BigDecimal changePercent=changeAmount.divide(currentPrice.getOpen(),4,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    .setScale(2,RoundingMode.HALF_UP);
        });

    }

}
