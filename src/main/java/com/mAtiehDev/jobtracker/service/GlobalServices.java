package com.mAtiehDev.jobtracker.service;


import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

@Service
public class GlobalServices {

    public Timestamp parseTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            LocalDate localDate = LocalDate.parse(dateStr); // parse YYYY-MM-DD
            return Timestamp.valueOf(localDate.atStartOfDay()); // convert to Timestamp
        } catch (Exception e) {
            throw new RuntimeException("Invalid date format: " + dateStr, e);
        }
    }
}

