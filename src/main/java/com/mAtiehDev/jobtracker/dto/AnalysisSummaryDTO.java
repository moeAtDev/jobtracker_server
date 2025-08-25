package com.mAtiehDev.jobtracker.dto;

//AnalysisSummaryDTO.java

import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisSummaryDTO {
 private String analysisId;
 private Timestamp analysisDate;
 private BigDecimal matchScore;
 private String jobTitle;
 private String companyName;
}
