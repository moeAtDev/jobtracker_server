package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.dto.EducationDTO;
import com.mAtiehDev.jobtracker.dto.ExperienceDTO;
import com.mAtiehDev.jobtracker.dto.PersonalDetailsDTO;
import com.mAtiehDev.jobtracker.dto.ProfileResponseDTO;
import com.mAtiehDev.jobtracker.dto.SkillDTO;
import com.mAtiehDev.jobtracker.model.UserSkill;
import com.mAtiehDev.jobtracker.service.JwtService;
import com.mAtiehDev.jobtracker.service.ProfileService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private JwtService jwtService;

    @GetMapping
    public ResponseEntity<ProfileResponseDTO> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token); // ✅ implement this
        ProfileResponseDTO profile = profileService.getUserProfile(userId);
        System.out.println("user id "+userId );
        System.out.println("edu "+profile.getEducation().size() );
        System.out.println("skill "+profile.getskill().size() );
        return ResponseEntity.ok(profile);
    }

    
    @PutMapping("/personal-details")
    public ResponseEntity<?> updatePersonalDetails(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PersonalDetailsDTO detailsDTO
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        
        profileService.updatePersonalDetails(userId, detailsDTO);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/update-summary")
    public ResponseEntity<?> updateSummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        String summary = request.get("summary");

        profileService.updateSummary(userId, summary);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/add-experience")
    public ResponseEntity<ExperienceDTO> addExperience(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ExperienceDTO dto
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        ExperienceDTO saved = profileService.addExperience(userId, dto);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/update-experience")
    public ResponseEntity<?> updateExperience(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ExperienceDTO dto
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        profileService.updateSingleExperience(userId, dto);
        return ResponseEntity.ok().build();
    }

    
    @DeleteMapping("/delete-experiences")
    public ResponseEntity<?> deleteExperiences(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<String> experienceIds
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        profileService.deleteExperiences(userId, experienceIds);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/update-education")
    public ResponseEntity<?> updateSingleEducation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody EducationDTO dto
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        profileService.updateSingleEducation(userId, dto);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/add-education")
    public ResponseEntity<EducationDTO> addEducation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody EducationDTO dto
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        EducationDTO saved = profileService.addNewEducation(userId, dto);
        return ResponseEntity.ok(saved);
    }


    
    
    @DeleteMapping("/delete-education/{id}")
    public ResponseEntity<?> deleteEducation(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        profileService.deleteEducation(userId, id);

        return ResponseEntity.ok().build();
    }

    
    @PostMapping("/add-skill")
    public ResponseEntity<?> addSkill(
        @RequestBody SkillDTO dto,
        @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String userId = jwtService.extractUserId(token);
        UserSkill savedSkill = profileService.addSkill(userId, dto);
        return ResponseEntity.ok(savedSkill);
    }

    @DeleteMapping("/delete-skill")
    public ResponseEntity<?> deleteSkill(
        @RequestParam String skillName,
        @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String userId = jwtService.extractUserId(token);
        profileService.deleteSkill(userId, skillName);
        return ResponseEntity.ok().build();
    }







}
