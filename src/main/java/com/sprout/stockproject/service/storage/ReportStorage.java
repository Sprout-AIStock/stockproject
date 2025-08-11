package com.sprout.stockproject.service.storage;

public interface ReportStorage {
    String saveToday(String markdown);
    String loadLatest();
    String loadByDate(String yyyymmdd);
}


