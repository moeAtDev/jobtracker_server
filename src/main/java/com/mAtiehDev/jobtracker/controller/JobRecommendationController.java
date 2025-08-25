package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.model.RecommendedJob;
import com.mAtiehDev.jobtracker.service.JobRecommendationService;
import com.mAtiehDev.jobtracker.service.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class JobRecommendationController {

    private final JobRecommendationService service;
    private final JwtService jwtService; // make sure you have this bean

    /** Get cached recommendations using token */
    @GetMapping
    public List<RecommendedJob> get(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        return service.getForUser(userId);
    }

    /** Force refresh using token */
    @PostMapping("/refresh")
    public List<RecommendedJob> refresh(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(name = "pages", defaultValue = "2") int pages) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        return service.refreshForUser(userId, Math.max(1, pages));
    }
}
