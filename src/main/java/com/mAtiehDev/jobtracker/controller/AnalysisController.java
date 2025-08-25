package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.dto.AnalysisRecordDTO;
import com.mAtiehDev.jobtracker.dto.AnalysisResponseDTO;
import com.mAtiehDev.jobtracker.dto.AnalysisSummaryDTO;
import com.mAtiehDev.jobtracker.dto.FullAnalysisDTO;
//import com.mAtiehDev.jobtracker.service.AnalysisService;
import com.mAtiehDev.jobtracker.service.AnalysisService2;
import com.mAtiehDev.jobtracker.service.JwtService;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/analyze")
@RequiredArgsConstructor
public class AnalysisController {

   // private final AnalysisService analysisService;
    private final JwtService jwtService;
    
    private final AnalysisService2 analysisService;

   /* @PostMapping
    public ResponseEntity<AnalysisResponseDTO> analyzeResumeAndJob(
        @RequestPart("resume") MultipartFile resumeFile,
        @RequestPart("jobDescription") String jobDescription,
        @RequestHeader("Authorization") String authHeader
    ) {
        // Extract token and user ID from Authorization header
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        AnalysisResponseDTO response = analysisService.analyze(resumeFile, jobDescription, userId);
        return ResponseEntity.ok(response);
    }*/
    
    @PostMapping
    public ResponseEntity<AnalysisResponseDTO> analyzeResumeAndJob(
        @RequestPart("resume") MultipartFile resumeFile,
        @RequestPart("jobDescription") String jobDescription,
        @RequestHeader("Authorization") String authHeader
    ) {
        // Extract token and user ID from Authorization header
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        AnalysisResponseDTO response = analysisService.analyze(resumeFile, jobDescription, userId);
        return ResponseEntity.ok(response);
    }
    
   /* @GetMapping("/records")
    public ResponseEntity<List<AnalysisRecordDTO>> getAllRecords(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        List<AnalysisRecordDTO> records = analysisService.getAllAnalysesByUserId(userId);
        return ResponseEntity.ok(records);
    }*/
    
    @GetMapping("/records")
    public ResponseEntity<List<AnalysisSummaryDTO>> getUserAnalysisSummaries(
    		 @RequestHeader("Authorization") String authHeader) {
        
    	 String token = authHeader.replace("Bearer ", "");
    	 
        String userId = jwtService.extractUserId(token); // implement this method
        List<AnalysisSummaryDTO> summaries = analysisService.getAnalysisSummariesByUserId(userId);
        return ResponseEntity.ok(summaries);
    }
    
    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<FullAnalysisDTO> getFullAnalysisById(@PathVariable String analysisId) {
        FullAnalysisDTO fullAnalysis = analysisService.getFullAnalysisById(analysisId);
        return ResponseEntity.ok(fullAnalysis);
    }


}
