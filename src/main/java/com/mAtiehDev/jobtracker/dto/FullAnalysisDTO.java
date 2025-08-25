package com.mAtiehDev.jobtracker.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

@Data
public class FullAnalysisDTO {
    private String analysisId;
    private BigDecimal matchScore;
    private Timestamp analysisDate;
    private List<String> missingSkills;
    private List<String> skillsToImprove;
    private List<String> recommendations;
    private String resumeRawText;
    private String jobDescriptionRawText;
    private String fileName;
    private String jobTitle;
    private String companyName;

   /* public FullAnalysisDTO(String analysisId, BigDecimal matchScore, Timestamp analysisDate,
                           List<String> missingSkills, List<String> skillsToImprove,
                           List<String> recommendations, String resumeRawText, String jobDescriptionRawText) {
        this.analysisId = analysisId;
        this.matchScore = matchScore;
        this.analysisDate = analysisDate;
        this.missingSkills = missingSkills;
        this.skillsToImprove = skillsToImprove;
        this.recommendations = recommendations;
        this.resumeRawText = resumeRawText;
        this.jobDescriptionRawText = jobDescriptionRawText;
    }*/

	public FullAnalysisDTO(String analysisId, BigDecimal matchScore, Timestamp analysisDate, List<String> missingSkills,
			List<String> skillsToImprove, List<String> recommendations, String resumeRawText,
			String jobDescriptionRawText, String fileName, String jobTitle, String companyName) {
		super();
		this.analysisId = analysisId;
		this.matchScore = matchScore;
		this.analysisDate = analysisDate;
		this.missingSkills = missingSkills;
		this.skillsToImprove = skillsToImprove;
		this.recommendations = recommendations;
		this.resumeRawText = resumeRawText;
		this.jobDescriptionRawText = jobDescriptionRawText;
		this.fileName = fileName;
		this.jobTitle = jobTitle;
		this.companyName = companyName;
	}

    
}
