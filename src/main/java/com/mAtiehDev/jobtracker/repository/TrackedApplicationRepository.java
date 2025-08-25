package com.mAtiehDev.jobtracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mAtiehDev.jobtracker.model.TrackedApplication;

public interface TrackedApplicationRepository extends JpaRepository<TrackedApplication, Long> {
    List<TrackedApplication> findByUserId(String userId);
    
    Optional<TrackedApplication> findByIdAndUserId(Long id, String userId);
    
 // Add this custom delete method:
    @Transactional
    void deleteByIdInAndUserId(List<Long> ids, String userId);
}