package com.mAtiehDev.jobtracker.repository;

import com.mAtiehDev.jobtracker.model.UserEducation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserEducationRepository extends JpaRepository<UserEducation, String> {
    List<UserEducation> findByUserId(String userId);
    
    List<UserEducation> findByUserIdOrderByEndDateDesc(String userId);
}