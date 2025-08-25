package com.mAtiehDev.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_table", schema="jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_id")
    private String userId; // Manually assigned
    
    @Column(name = "email_address")
    private String emailAddress;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "password")
    private String password;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "user_header")
    private String userHeader;
    
    @Column(name = "user_city")
    private String userCity;
    
    @Column(name = "user_country")
    private String userCountry;
    
    @Column(name = "summary" ,columnDefinition = "TEXT")
    private String summary;
}

