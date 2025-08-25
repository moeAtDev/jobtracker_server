package com.mAtiehDev.jobtracker.repository;

import com.mAtiehDev.jobtracker.model.UserExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserExperienceRepository extends JpaRepository<UserExperience, String> {
    List<UserExperience> findByUserId(String userId);
    
    // ✅ New method: Sorted by endDate descending (newest first)
    List<UserExperience> findByUserIdOrderByEndDateDesc(String userId);
}