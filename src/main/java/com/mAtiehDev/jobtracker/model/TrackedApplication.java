package com.mAtiehDev.jobtracker.model;

import java.time.Instant;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tracked_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    
    private String jobTitle;
    
    private String company;
    
    @Column(name = "application_source")
    private String source; // e.g. LinkedIn, Indeed
    
    private Instant appliedAt;
    
    private String emailSnippet;
    
    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;
    
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "resume_file_data", columnDefinition = "bytea")
    private byte[] resumeFileData;

    public byte[] getResumeFileData() {
        return resumeFileData;
    }

    public void setResumeFileData(byte[] resumeFileData) {
        this.resumeFileData = resumeFileData;
    }


    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "resume_file_type")
    private String resumeFileType;
}
