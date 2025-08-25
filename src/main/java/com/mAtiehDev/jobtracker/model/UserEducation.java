package com.mAtiehDev.jobtracker.model;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_education", schema="jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEducation  {

    @Id
    @Column(name = "Id")
    private String Id; // Manually assigned
    
    @Column(name = "user_id")
    private String userId; 
    
    @Column(name = "user_name")
    private String userName; 
    
    @Column(name = "degree_type")
    private String degreeType;
    
    @Column(name = "major")
    private String major;
    
    @Column(name = "university_name")
    private String universityName;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "start_date")
    private Timestamp startDate;
    
    @Column(name = "end_date")
    private Timestamp endDate;
    
    
    

}

