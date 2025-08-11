package com.sprout.stockproject.repository;

import com.sprout.stockproject.entity.StockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockInfoRepository extends JpaRepository<StockInfo, Long> {

    Optional<StockInfo> findByStockName(String stockName);

    Optional<StockInfo> findByStockCode(String stockCode);

    boolean existsByStockCode(String stockCode);

    List<StockInfo> findByStockNameContaining(String keyword);

    @Query("SELECT s FROM StockInfo s WHERE s.stockName LIKE %:keyword% OR s.stockCode LIKE %:keyword%")
    List<StockInfo> findByKeyword(@Param("keyword") String keyword);
}
