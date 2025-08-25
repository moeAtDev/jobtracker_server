package com.mAtiehDev.jobtracker.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gmail_tokens")

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GmailToken {
    @Id
    private String userId;

    private String accessToken;
    private String refreshToken;
    private Instant expiryTime;
    private Instant lastModifiedTime; 
}
