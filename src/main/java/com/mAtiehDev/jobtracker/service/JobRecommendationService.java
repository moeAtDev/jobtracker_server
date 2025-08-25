package com.mAtiehDev.jobtracker.service;

import com.mAtiehDev.jobtracker.model.UserSkill;
import com.mAtiehDev.jobtracker.service.JoobleClient;

import jakarta.transaction.Transactional;

import com.mAtiehDev.jobtracker.dto.JobDTO;
import com.mAtiehDev.jobtracker.model.RecommendedJob;
import com.mAtiehDev.jobtracker.model.User;
import com.mAtiehDev.jobtracker.repository.RecommendedJobRepository;
import com.mAtiehDev.jobtracker.repository.UserRepository;
import com.mAtiehDev.jobtracker.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobRecommendationService {

    private final UserSkillRepository userSkillRepository;
    private final RecommendedJobRepository recommendedJobRepository;
    private final JoobleClient joobleClient;
    private final UserRepository userRepository; // ✅ Add this


    /** Build keyword string from user skills (ex: "Java React Spring Boot") */
   /* private String buildSkillKeywords(String userId) {
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);
        //also include location
        return skills.stream()
                .map(UserSkill::getSkillName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(" "));
    }*/
    
   /* private String buildSkillKeywords(String userId, String location) {
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);

        // Collect skill names
        String skillKeywords = skills.stream()
                .map(UserSkill::getSkillName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(" "));

        // If no skills, use general keywords
        if (skillKeywords.isEmpty()) {
            skillKeywords = "general professional skills"; // <-- replace with your preferred general keywords
        }

        // Include location if provided
        if (location != null && !location.isBlank()) {
            skillKeywords += " " + location.trim();
        }

        return skillKeywords;
    } */
    
    private String buildSkillKeywords(String userId) {
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);

        String skillKeywords = skills.stream()
                .map(UserSkill::getSkillName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(" "));

        if (skillKeywords.isEmpty()) {
            skillKeywords = "general professional skills";
        }
        return skillKeywords;
    }
    
    private String getUserLocation(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "";

        String location = "";
        if (user.getUserCity() != null && !user.getUserCity().isBlank()) {
            location += user.getUserCity().trim();
        }
        if (user.getUserCountry() != null && !user.getUserCountry().isBlank()) {
            if (!location.isEmpty()) location += ", ";
            location += user.getUserCountry().trim();
        }
        return location;
    }


    /*
    private String buildSkillKeywords(String userId) {
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);

        String skillKeywords = skills.stream()
                .map(UserSkill::getSkillName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(" "));

        if (skillKeywords.isEmpty()) {
            skillKeywords = "general professional skills";
        }

        // 🔹 Fetch user's city/country from DB
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            String location = "";
            if (user.getUserCity() != null && !user.getUserCity().isBlank()) {
                location += user.getUserCity().trim();
            }
            if (user.getUserCountry() != null && !user.getUserCountry().isBlank()) {
                if (!location.isEmpty()) location += " ";
                location += user.getUserCountry().trim();
            }

            if (!location.isEmpty()) {
                skillKeywords += " " + location;
            }
        }

        return skillKeywords;
    }
*/


    /** Force refresh from Jooble and replace cache */
    @Transactional
    public List<RecommendedJob> refreshForUser(String userId, int pages) {
    	   String keywords = buildSkillKeywords(userId);
    	    String location = getUserLocation(userId);

    	    List<JobDTO> all = new ArrayList<>();
    	    for (int p = 1; p <= pages; p++) {
    	        all.addAll(joobleClient.searchJobs(keywords, location, p));
    	    }

        // naive dedupe by title|company
        Map<String, JobDTO> dedup = new LinkedHashMap<>();
        for (JobDTO j : all) {
            String key = ((j.getTitle() == null ? "" : j.getTitle()) + "|" +
                          (j.getCompany() == null ? "" : j.getCompany())).toLowerCase();
            dedup.putIfAbsent(key, j);
        }

        // replace cache
        recommendedJobRepository.deleteByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now();

        List<RecommendedJob> saved = new ArrayList<>();
        for (JobDTO dto : dedup.values()) {
            RecommendedJob r = RecommendedJob.builder()
                    .userId(userId)
                    .title(dto.getTitle())
                    .company(dto.getCompany())
                    .location(dto.getLocation())
                    .snippet(dto.getSnippet())
                    .url(dto.getUrl())
                    .salary(dto.getSalary())
                    .fetchedAt(now)
                    .build();
            saved.add(recommendedJobRepository.save(r));
        }
        return saved;
    }

    /** Get cached recommendations; if none, refresh 2 pages and return */
    public List<RecommendedJob> getForUser(String userId) {
        List<RecommendedJob> cached = recommendedJobRepository.findByUserIdOrderByFetchedAtDesc(userId);
        if (cached.isEmpty()) {
            return refreshForUser(userId, 2);
        }
        return cached;
    }
}
