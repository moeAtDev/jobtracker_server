package com.mAtiehDev.jobtracker.repository;


import com.mAtiehDev.jobtracker.model.RecommendedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendedJobRepository extends JpaRepository<RecommendedJob, Long> {
    List<RecommendedJob> findByUserIdOrderByFetchedAtDesc(String userId);
    void deleteByUserId(String userId);
}
