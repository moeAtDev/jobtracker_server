package com.mAtiehDev.jobtracker.model;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_skill", schema="jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSkill  {

    @Id
    @Column(name = "Id")
    private String Id; // Manually assigned
    
    @Column(name = "user_id")
    private String userId; 
    
    @Column(name = "user_name")
    private String userName; 
    
    
    @Column(name = "skill_name")
    private String skillName; 
    
    
    
    
    

}

