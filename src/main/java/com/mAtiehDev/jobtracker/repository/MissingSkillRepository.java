package com.mAtiehDev.jobtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mAtiehDev.jobtracker.model.MissingSkill;

@Repository
public interface MissingSkillRepository extends JpaRepository<MissingSkill, String> {
	List<MissingSkill> findByAnalysis_AnalysisId(String analysisId);
}
