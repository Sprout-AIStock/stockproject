package com.sprout.stockproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stockinfo")
public class StockInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_name", length = 100, nullable = false)
    private String stockName;

    @Column(name = "stock_code", length = 20, nullable = false, unique = true)
    private String stockCode;

    public StockInfo() {
    }

    public StockInfo(String stockName, String stockCode) {
        this.stockName = stockName;
        this.stockCode = stockCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }
}
