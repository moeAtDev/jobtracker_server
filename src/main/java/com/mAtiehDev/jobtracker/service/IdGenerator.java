package com.mAtiehDev.jobtracker.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdGenerator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String generateId(String prefix, String sequenceName) {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('" + sequenceName + "')", Long.class);
        return prefix + String.format("%04d", nextVal); // e.g., user_2025_0001
    }
}
