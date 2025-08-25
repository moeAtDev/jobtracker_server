package com.mAtiehDev.jobtracker.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills_to_improve", schema = "jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillToImprove {

    @Id
    private String improveSkillId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false)
    private String skillName;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
