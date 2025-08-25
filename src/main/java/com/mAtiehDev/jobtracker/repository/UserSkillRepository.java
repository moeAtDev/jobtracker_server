package com.mAtiehDev.jobtracker.repository;

import com.mAtiehDev.jobtracker.model.UserSkill;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, String> {
    List<UserSkill> findByUserId(String userId);
    
    boolean existsByUserIdAndSkillName(String userId, String skillName);

    Optional<UserSkill> findByUserIdAndSkillName(String userId, String skillName);
}