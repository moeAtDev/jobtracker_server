package com.mAtiehDev.jobtracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendations", schema = "jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    
	 @Id
    private String recommendationId;
	 
    @ManyToOne
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    private String recommendationText;
}
