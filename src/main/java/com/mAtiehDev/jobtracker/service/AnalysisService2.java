package com.mAtiehDev.jobtracker.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mAtiehDev.jobtracker.dto.AnalysisRecordDTO;
import com.mAtiehDev.jobtracker.dto.AnalysisResponseDTO;
import com.mAtiehDev.jobtracker.dto.AnalysisSummaryDTO;
import com.mAtiehDev.jobtracker.dto.FullAnalysisDTO;
import com.mAtiehDev.jobtracker.model.Analysis;
import com.mAtiehDev.jobtracker.model.MissingSkill;
import com.mAtiehDev.jobtracker.model.Recommendation;
import com.mAtiehDev.jobtracker.model.SkillToImprove;
import com.mAtiehDev.jobtracker.repository.AnalysisRepository;
import com.mAtiehDev.jobtracker.repository.MissingSkillRepository;
import com.mAtiehDev.jobtracker.repository.RecommendationRepository;
import com.mAtiehDev.jobtracker.repository.SkillToImproveRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService2 {

    private final Tika tika = new Tika();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final AnalysisSaveService analysisSaveService;
    private final AnalysisRepository analysisRepository;
    private final MissingSkillRepository missingSkillRepository;
    private final RecommendationRepository recommendationRepository;
    private final SkillToImproveRepository skillToImproveRepository;

    @Value("${together.api.key}")
    private String togetherApiKey;

    public AnalysisResponseDTO analyze(MultipartFile resumeFile, String jobDescription, String userId) {
        try {
            String parsedResume = extractTextFromFile(resumeFile);
            String promptString = buildPrompt(parsedResume, jobDescription);

            String aiResponse = callTogetherAI(promptString);

            AnalysisResponseDTO dto = parseAiResponse(aiResponse, userId);
            dto.setFileName(resumeFile.getOriginalFilename());

            // Save analysis and related entities to DB
            analysisSaveService.saveAnalysis(userId, dto,parsedResume,jobDescription);

            return dto;
        } catch (Exception e) {
            log.error("Analysis failed", e);
            throw new RuntimeException("Analysis failed", e);
        }
    }

    private String extractTextFromFile(MultipartFile file) throws TikaException {
        try {
            return tika.parseToString(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse resume file.", e);
        }
    }

   /*with no title and company extraction 
    *  private String buildPrompt(String resumeText, String jobDesc) {
        return """
            Analyze the candidate's resume below and compare it with the job description.
            Identify:

            - A match score (as a percentage from 0 to 100) indicating how well the resume fits the job.
            - A list of missing skills that are required by the job but not found in the resume.
            - A list of skills the candidate has but should improve for this job.
            - Recommendations on how the candidate can improve their resume or skillset.

            Resume:
            %s

            Job Description:
            %s

            Return ONLY valid JSON in the following format (your values must be based on the analysis):

            {
              "matchScore": [number],
              "missingSkills": [list of strings],
              "skillsToImprove": [list of strings],
              "recommendations": [list of strings]
            }

            Do not include any explanations or text outside of the JSON.
            """.formatted(resumeText, jobDesc);
    }*/
    
    private String buildPrompt(String resumeText, String jobDesc) {
        return """
            Analyze the candidate's resume below and compare it with the job description.
            Identify:

            - A match score (as a percentage from 0 to 100) indicating how well the resume fits the job.
            - A list of missing skills that are required by the job but not found in the resume.
            - A list of skills the candidate has but should improve for this job.
            - Recommendations on how the candidate can improve their resume or skillset.
            - Extract the job title and company name from the job description.

            Resume:
            %s

            Job Description:
            %s

            Return ONLY valid JSON in the following format (your values must be based on the analysis):

            {
              "matchScore": [number],
              "missingSkills": [list of strings],
              "skillsToImprove": [list of strings],
              "recommendations": [list of strings],
              "jobTitle": "[string]",
              "companyName": "[string]"
            }

            Do not include any explanations or text outside of the JSON.
            """.formatted(resumeText, jobDesc);
    }


    private String callTogetherAI(String prompt) {
        String url = "https://api.together.xyz/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(togetherApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "mistralai/Mixtral-8x7B-Instruct-v0.1");
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").get(0).path("message").path("content").asText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Together AI response", e);
            }
        } else {
            throw new RuntimeException("Together AI request failed: " + response.getStatusCode());
        }
    }

    private AnalysisResponseDTO parseAiResponse(String json, String userId) {
        try {
            String cleanedJson = cleanJsonString(json);
            return objectMapper.readValue(cleanedJson, AnalysisResponseDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response JSON", e);
            throw new RuntimeException("Invalid AI response format", e);
        }
    }

    private String cleanJsonString(String json) {
        if (json == null) return null;

        String trimmed = json.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }

        return trimmed;
    }
    
    //record
  /*noo usage this service get all data of analysis elated to user using AnalysisRecordDTO
   *   public List<AnalysisRecordDTO> getAllAnalysesByUserId(String userId) {
        List<Analysis> analyses = analysisRepository.findByUserId(userId);

        return analyses.stream().map(analysis -> {
            String analysisId = analysis.getAnalysisId();

            List<String> missingSkills = missingSkillRepository.findByAnalysis_AnalysisId(analysisId)
                    .stream().map(MissingSkill::getSkillName).collect(Collectors.toList());

            List<String> skillsToImprove = skillToImproveRepository.findByAnalysis_AnalysisId(analysisId)
                    .stream().map(SkillToImprove::getSkillName).collect(Collectors.toList());

            List<String> recommendations = recommendationRepository.findByAnalysis_AnalysisId(analysisId)
                    .stream().map(Recommendation::getRecommendationText).collect(Collectors.toList());

            return new AnalysisRecordDTO(
                    analysisId,
                    analysis.getMatchScore(),
                    analysis.getAnalysisDate(),
                    missingSkills,
                    skillsToImprove,
                    recommendations
            );
        }).collect(Collectors.toList());
    }
    */
    
    public FullAnalysisDTO getFullAnalysisById(String analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Analysis not found with ID: " + analysisId));

        List<String> missingSkills = missingSkillRepository.findByAnalysis_AnalysisId(analysisId)
                .stream().map(MissingSkill::getSkillName).toList();

        List<String> skillsToImprove = skillToImproveRepository.findByAnalysis_AnalysisId(analysisId)
                .stream().map(SkillToImprove::getSkillName).toList();

        List<String> recommendations = recommendationRepository.findByAnalysis_AnalysisId(analysisId)
                .stream().map(Recommendation::getRecommendationText).toList();

        return new FullAnalysisDTO(
                analysis.getAnalysisId(),
                analysis.getMatchScore(),
                analysis.getAnalysisDate(),
                missingSkills,
                skillsToImprove,
                recommendations,
                analysis.getResumeRawText(),
                analysis.getJobDescriptionRawText(),
                analysis.getFileName(),
                analysis.getCompanyName(),
                analysis.getJobTitle()
        );
    }

    public List<AnalysisSummaryDTO> getAnalysisSummariesByUserId(String userId) {
        List<Analysis> analyses = analysisRepository.findByUserId(userId);
        return analyses.stream()
                .map(a -> new AnalysisSummaryDTO(
                        a.getAnalysisId(),
                        a.getAnalysisDate(),
                        a.getMatchScore(),
                        a.getJobTitle(),
                        a.getCompanyName()))
                .collect(Collectors.toList());
    }

}
