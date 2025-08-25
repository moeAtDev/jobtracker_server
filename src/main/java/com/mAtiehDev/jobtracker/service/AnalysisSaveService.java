package com.mAtiehDev.jobtracker.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.mAtiehDev.jobtracker.dto.AnalysisResponseDTO;
import com.mAtiehDev.jobtracker.model.Analysis;
import com.mAtiehDev.jobtracker.model.MissingSkill;
import com.mAtiehDev.jobtracker.model.Recommendation;
import com.mAtiehDev.jobtracker.model.SkillToImprove;
import com.mAtiehDev.jobtracker.repository.AnalysisRepository;
import com.mAtiehDev.jobtracker.repository.MissingSkillRepository;
import com.mAtiehDev.jobtracker.repository.RecommendationRepository;
import com.mAtiehDev.jobtracker.repository.SkillToImproveRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisSaveService {

    private final AnalysisRepository analysisRepository;
    private final MissingSkillRepository missingSkillRepository;
    private final SkillToImproveRepository skillToImproveRepository;
    private final RecommendationRepository recommendationRepository;
    private final IdGenerator idGenerator;

    public void saveAnalysis(String userId, AnalysisResponseDTO dto,String Resume,String jobDescription) {
        // Step 1: Create and save Analysis
        String analysisId = idGenerator.generateId("ana_2025_", "jobtracker_data.analysis_seq");

        Analysis analysis = new Analysis();
        analysis.setAnalysisId(analysisId);
        analysis.setUserId(userId);
        analysis.setMatchScore(BigDecimal.valueOf(dto.getMatchScore()));
        analysis.setAnalysisDate(Timestamp.valueOf(LocalDateTime.now()));
        analysis.setJobDescriptionRawText(jobDescription);
        analysis.setResumeRawText(Resume);
        analysis.setJobTitle(dto.getJobTitle());
        analysis.setCompanyName(dto.getCompanyName());
        analysis.setFileName(dto.getFileName());

        analysisRepository.save(analysis);

        // Step 2: Save missing skills
        dto.getMissingSkills().forEach(skillName -> {
            MissingSkill ms = new MissingSkill();
            ms.setMissingSkillId(idGenerator.generateId("miss_2025_", "jobtracker_data.missing_skill_seq"));
            ms.setAnalysis(analysis);
            ms.setSkillName(skillName);
            missingSkillRepository.save(ms);
        });

        // Step 3: Save skills to improve
        dto.getSkillsToImprove().forEach(skill -> {
            SkillToImprove si = new SkillToImprove();
            si.setImproveSkillId(idGenerator.generateId("impr_2025_", "jobtracker_data.skill_improve_seq"));
            si.setAnalysis(analysis);
            si.setSkillName(skill);
            si.setReason("Recommended improvement for better match."); // or use AI explanation
            skillToImproveRepository.save(si);
        });

        // Step 4: Save recommendations
        dto.getRecommendations().forEach(rec -> {
            Recommendation r = new Recommendation();
            r.setRecommendationId(idGenerator.generateId("rec_2025_", "jobtracker_data.recommendation_seq"));
            r.setAnalysis(analysis);
            r.setRecommendationText(rec);
            recommendationRepository.save(r);
        });
    }
}

