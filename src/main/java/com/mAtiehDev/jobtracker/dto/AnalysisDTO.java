package com.mAtiehDev.jobtracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import lombok.Data;
//not used
//to be deleted 


@Data
public class AnalysisDTO {
    private String analysisId;
    private String userId;
    private LocalDateTime analysisDate;
    private BigDecimal matchScore;;
    private String fileName;
    private String jobTitle;
    private String companyName;
    
 
}
