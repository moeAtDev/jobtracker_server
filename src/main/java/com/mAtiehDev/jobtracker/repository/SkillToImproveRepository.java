package com.mAtiehDev.jobtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mAtiehDev.jobtracker.model.SkillToImprove;

@Repository
public interface SkillToImproveRepository extends JpaRepository<SkillToImprove, String> {
	List<SkillToImprove> findByAnalysis_AnalysisId(String analysisId);
}
