package com.mAtiehDev.jobtracker.model;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_experience", schema="jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExperience  {

    @Id
    @Column(name = "job_id")
    private String jobId; // Manually assigned
    
    @Column(name = "user_id")
    private String userId; 
    
    @Column(name = "user_name")
    private String userName; 
    
    @Column(name = "job_title")
    private String jobTitle;
    
    @Column(name = "job_description" ,columnDefinition = "TEXT")
    private String jobDescription;
    
    @Column(name = "job_type")
    private String jobType;
    
    @Column(name = "company_name")
    private String companyName;
    
    @Column(name = "company_city")
    private String companyCity;
    
    @Column(name = "company_country")
    private String companyCountry;
    
    @Column(name = "start_date")
    private Timestamp startDate;
    
    @Column(name = "end_date")
    private Timestamp endDate;
    
    
    

}

