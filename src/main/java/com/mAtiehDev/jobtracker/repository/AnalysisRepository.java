package com.mAtiehDev.jobtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mAtiehDev.jobtracker.model.Analysis;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, String> {
    List<Analysis> findByUserId(String userId);
}