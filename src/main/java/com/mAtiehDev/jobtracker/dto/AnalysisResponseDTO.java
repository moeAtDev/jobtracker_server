package com.mAtiehDev.jobtracker.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponseDTO {
    private double matchScore;
    private List<String> missingSkills;
    private List<String> skillsToImprove;
    private List<String> recommendations;
    private String jobTitle;
    private String companyName;
    private String fileName;
}
