package com.mAtiehDev.jobtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mAtiehDev.jobtracker.model.Recommendation;


public interface RecommendationRepository extends JpaRepository<Recommendation, String> {
	 List<Recommendation> findByAnalysis_AnalysisId(String analysisId);


}
