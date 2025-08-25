package com.mAtiehDev.jobtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recommended_jobs", schema = "jobtracker_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private String userId;

    private String title;
    private String company;
    private String location;

    @Column(length = 4000)
    private String snippet;

    @Column(length = 1024)
    private String url;

    private String salary;

    @Column(name="fetched_at", columnDefinition = "timestamptz")
    private OffsetDateTime fetchedAt;
}
