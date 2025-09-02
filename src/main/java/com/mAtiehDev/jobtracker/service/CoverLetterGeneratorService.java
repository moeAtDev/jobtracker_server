// src/main/java/com/mAtiehDev/jobtracker/service/CoverLetterGeneratorService.java
package com.mAtiehDev.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mAtiehDev.jobtracker.dto.CoverLetterRequestDTO;
import com.mAtiehDev.jobtracker.dto.CoverLetterResponseDTO;
import com.mAtiehDev.jobtracker.repository.UserEducationRepository;
import com.mAtiehDev.jobtracker.repository.UserExperienceRepository;
import com.mAtiehDev.jobtracker.repository.UserRepository;
import com.mAtiehDev.jobtracker.repository.UserSkillRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.apache.poi.xwpf.usermodel.*;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoverLetterGeneratorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${together.api.key}")
    private String apiKey;

    private static final String URL = "https://api.together.xyz/v1/chat/completions";
    private static final String MODEL = "mistralai/Mixtral-8x7B-Instruct-v0.1";

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;

    // -------------------- Public API --------------------

    public CoverLetterResponseDTO generateCoverLetter(CoverLetterRequestDTO req) throws TikaException {
        String baseText = getUnifiedCvText(req);
        String prompt = buildPrompt(baseText, req.getJobDescription());

        String generated = generateText(prompt);

        CoverLetterResponseDTO out = new CoverLetterResponseDTO();
        out.setGeneratedCoverLetter(generated);
        return out;
    }
    
    public byte[] exportCoverLetterToPdf(String coverLetterText) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 14f;
            float width = page.getMediaBox().getWidth() - 2 * margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setFont(PDType1Font.HELVETICA, 12);

            try {
                // Split paragraphs by new lines
                for (String paragraph : coverLetterText.split("\\r?\\n")) {
                    if (paragraph.trim().isEmpty()) {
                        // Add empty line for paragraph spacing
                        y -= lineHeight;
                        continue;
                    }

                    // Break paragraph into words
                    String[] words = paragraph.split(" ");
                    StringBuilder lineBuilder = new StringBuilder();

                    for (String word : words) {
                        String testLine = lineBuilder.length() == 0 ? word : lineBuilder + " " + word;
                        float size = 12f;
                        float textWidth = (PDType1Font.HELVETICA.getStringWidth(testLine) / 1000) * size;

                        if (textWidth > width) {
                            // Draw current line
                            if (y < margin) {
                                cs.close(); // close old stream

                                // Add new page
                                page = new PDPage(PDRectangle.LETTER);
                                doc.addPage(page);
                                y = page.getMediaBox().getHeight() - margin;

                                // New stream for new page
                                cs = new PDPageContentStream(doc, page);
                                cs.setFont(PDType1Font.HELVETICA, 12);
                            }
                            cs.beginText();
                            cs.newLineAtOffset(margin, y);
                            cs.showText(lineBuilder.toString());
                            cs.endText();
                            y -= lineHeight;

                            // Start new line with the word that didn’t fit
                            lineBuilder = new StringBuilder(word);
                        } else {
                            lineBuilder = new StringBuilder(testLine);
                        }
                    }

                    // Draw the last line of the paragraph
                    if (lineBuilder.length() > 0) {
                        if (y < margin) {
                            cs.close(); // close old stream

                            page = new PDPage(PDRectangle.LETTER);
                            doc.addPage(page);
                            y = page.getMediaBox().getHeight() - margin;

                            cs = new PDPageContentStream(doc, page);
                            cs.setFont(PDType1Font.HELVETICA, 12);
                        }
                        cs.beginText();
                        cs.newLineAtOffset(margin, y);
                        cs.showText(lineBuilder.toString());
                        cs.endText();
                        y -= lineHeight;
                    }

                    // Add extra spacing after each paragraph
                    y -= lineHeight;
                }
            } finally {
                cs.close(); // ensure last stream is closed
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }



   /* public byte[] exportCoverLetterToPdf(String coverLetterText) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50f;
                float y = page.getMediaBox().getHeight() - margin;
                float lineHeight = 14f;

                for (String line : coverLetterText.split("\\r?\\n")) {
                    if (y < margin) {
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(line);
                    cs.endText();
                    y -= lineHeight;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }*/

    public byte[] exportCoverLetterToWord(String coverLetterText) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            for (String line : coverLetterText.split("\\r?\\n")) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line);
                run.setFontSize(12);
                run.setFontFamily("Calibri");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    // -------------------- Internal helpers --------------------

    private String getUnifiedCvText(CoverLetterRequestDTO req) throws TikaException {
        String rawText;
        if (req.isUseUploadedCv() && req.getUploadedCv() != null && !req.getUploadedCv().isEmpty()) {
            rawText = parseUploadedCv(req.getUploadedCv());
        } else {
            rawText = buildUserProfileText(req.getUserId());
        }
        return normalizeText(rawText);
    }

    private String parseUploadedCv(MultipartFile cv) throws TikaException {
        try {
            Tika tika = new Tika();
            return tika.parseToString(cv.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse uploaded CV", e);
        }
    }

    private String buildPrompt(String userData, String jobDescription) {
        return """
            Act as a professional Canadian cover letter writer. Write a compelling, ATS-friendly cover letter
            tailored to this user's background and the job description. Do NOT invent experiences.
            USER DATA:
            %s

            JOB DESCRIPTION:
            %s
            """.formatted(userData, jobDescription);
    }

    private String generateText(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("messages", new Object[]{
                    Map.of("role", "system", "content", "You are an expert cover letter writer."),
                    Map.of("role", "user", "content", prompt)
            });
            body.put("temperature", 0.3);
            body.put("max_tokens", 1200);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").get(0).path("message").path("content").asText();
            } else {
                throw new RuntimeException("Together AI request failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Together AI", e);
            throw new RuntimeException("Failed to generate text with Together AI", e);
        }
    }

    private String normalizeText(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("^[\\-*•]\\s*", "• ");
    }

    private String buildUserProfileText(String userId) {
        var user = userRepository.findById(userId).orElseThrow();
        var exps = userExperienceRepository.findByUserId(userId);
        var edus = userEducationRepository.findByUserId(userId);
        var skills = userSkillRepository.findByUserId(userId).stream()
                .map(s -> s.getSkillName()).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(user.getUserName()).append("\n")
          .append(user.getUserCity()).append(" | ")
          .append(user.getEmailAddress()).append("\n\n");

        sb.append("PROFILE\n");
        sb.append(user.getSummary() == null ? "" : user.getSummary()).append("\n\n");

        sb.append("WORK EXPERIENCE\n");
        for (var e : exps) {
            sb.append("JOBTITLE:").append(e.getJobTitle()).append("\n");
            sb.append("COMPANY:").append(e.getCompanyName()).append("\n");
            sb.append("DATES:").append(e.getStartDate())
              .append(" – ").append(e.getEndDate() == null ? "Present" : e.getEndDate())
              .append(" / ").append(e.getCompanyCity() == null ? "Remote" : e.getCompanyCity())
              .append("\n");
            if (e.getJobDescription() != null) {
                for (String bullet : e.getJobDescription().split("\n")) {
                    sb.append("• ").append(bullet.trim()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("EDUCATION\n");
        for (var ed : edus) {
            sb.append("DEGREE:").append(ed.getDegreeType()).append("\n");
            sb.append("SCHOOL:").append(ed.getUniversityName())
              .append(", ").append(ed.getCity())
              .append(" | ").append(ed.getEndDate()).append("\n\n");
        }

        sb.append("SKILLS\n");
        sb.append(String.join(", ", skills)).append("\n");

        return sb.toString();
    }
}
