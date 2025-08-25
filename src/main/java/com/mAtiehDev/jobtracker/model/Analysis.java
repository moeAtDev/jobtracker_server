package com.mAtiehDev.jobtracker.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis", schema = "jobtracker_data")
@Data  // generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // no-arg constructor (required by JPA)
@AllArgsConstructor // all-args constructor
public class Analysis {
    @Id
    @Column(name = "analysis_id")
    private String analysisId;

    @Column(name ="user_id",nullable = false)
    private String userId;

    
    @Column(name = "analysis_date")
    private Timestamp analysisDate;

    @Column(name = "match_score")
    private BigDecimal matchScore;
    
    @Column(name = "resume_raw_text", columnDefinition = "TEXT")
    private String resumeRawText;

    @Column(name = "job_description_raw_text", columnDefinition = "TEXT")
    private String jobDescriptionRawText;
    
    
    @Column(name ="file_name")
    private String fileName;
    
    @Column(name ="job_title")
    private String jobTitle;
    
    @Column(name ="company_name")
    private String companyName;

    
}
