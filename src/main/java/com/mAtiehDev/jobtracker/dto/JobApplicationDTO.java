package com.mAtiehDev.jobtracker.dto;


import java.time.Instant;


import lombok.Data;

@Data
public class JobApplicationDTO {
    
    
    private Long id;
    private String jobTitle;
    private String company;
    private String source;
    private Instant appliedAt;
    private String emailSnippet;
    private String userId;
}
