package com.mAtiehDev.jobtracker.service;
/*
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mAtiehDev.jobtracker.dto.AnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.ChatMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ChatClient chatClient;
    private final Tika tika = new Tika();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResponseDTO analyze(MultipartFile resumeFile, String jobDescription, String userId) {
        try {
            String parsedResume = extractTextFromFile(resumeFile);
            String promptString = buildPrompt(parsedResume, jobDescription);

            ChatMessage userMessage = new ChatMessage(MessageType.USER, promptString);

            // FIXED: Prompt has no builder(), use constructor instead
            Prompt prompt = new Prompt(List.of(userMessage));

            String aiResponse = chatClient.call(prompt)
                                          .getResult()
                                          .getOutput()
                                          .getContent();

            return parseAiResponse(aiResponse, userId);
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
    private String buildPrompt(String resumeText, String jobDesc) {
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
    }


    private AnalysisResponseDTO parseAiResponse(String json, String userId) {
        try {
            // Remove surrounding markdown code block markers (e.g. ```json ... ``` or ```)
            String cleanedJson = cleanJsonString(json);
            return objectMapper.readValue(cleanedJson, AnalysisResponseDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response JSON", e);
            throw new RuntimeException("Invalid AI response format", e);
        }
    }

    
    /// * Removes markdown code fences and trims the string to isolate JSON.
     
    private String cleanJsonString(String json) {
        if (json == null) return null;

        // Trim whitespace
        String trimmed = json.trim();

        // Remove code fences (``` or ```json) if present
        if (trimmed.startsWith("```")) {
            // Remove first line with ```
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }

            // Remove last ``` line
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }

            trimmed = trimmed.trim();
        }

        return trimmed;
    }

}*/
