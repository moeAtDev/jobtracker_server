package com.mAtiehDev.jobtracker.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
//this need to be deleted it is only used to return all records and their details which we dont use
//to be deleted 

@Data
@AllArgsConstructor
public class AnalysisRecordDTO {
    private String analysisId;
    private BigDecimal matchScore;
    private Timestamp analysisDate;
   /* private String fileName;
    private String jobTitle;
    private String companyName;*/

    private List<String> missingSkills;
    private List<String> skillsToImprove;
    private List<String> recommendations;
}

