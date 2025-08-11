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

    /** 종목명으로 정확히 일치하는 주식 조회 */
    Optional<StockInfo> findByStockName(String stockName);

    /** 종목코드로 정확히 일치하는 주식 조회 */
    Optional<StockInfo> findByStockCode(String stockCode);

    /** 종목코드 존재 여부 확인 (중복 체크용) */
    boolean existsByStockCode(String stockCode);

    /** 종목명에서 키워드 포함 검색 */
    List<StockInfo> findByStockNameContaining(String keyword);

    /** 종목명 또는 종목코드에서 키워드 통합 검색 */
    @Query("SELECT s FROM StockInfo s WHERE s.stockName LIKE %:keyword% OR s.stockCode LIKE %:keyword%")
    List<StockInfo> findByKeyword(@Param("keyword") String keyword);
}
