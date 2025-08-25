package com.mAtiehDev.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "missing_skills", schema = "jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissingSkill {

    @Id
    private String missingSkillId;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false)
    private String skillName;
}
