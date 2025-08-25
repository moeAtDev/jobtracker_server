// src/main/java/com/mAtiehDev/jobtracker/service/CvGeneratorService.java
package com.mAtiehDev.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mAtiehDev.jobtracker.dto.CvGenerationRequestDTO;
import com.mAtiehDev.jobtracker.dto.CvGenerationResponseDTO;
import com.mAtiehDev.jobtracker.repository.UserEducationRepository;
import com.mAtiehDev.jobtracker.repository.UserExperienceRepository;
import com.mAtiehDev.jobtracker.repository.UserRepository;
import com.mAtiehDev.jobtracker.repository.UserSkillRepository;

import org.apache.poi.xwpf.usermodel.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvGeneratorService {
	//togather  AI requirement 
	private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String URL = "https://api.together.xyz/v1/chat/completions";
    private static final String MODEL = "mistralai/Mixtral-8x7B-Instruct-v0.1";

    @Value("${together.api.key}")
    private String apiKey;
    
    //Repository
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;

   // private final UserDetailsService userService;           
    //private final TogetherAiService togetherAiService;

    public CvGenerationResponseDTO generateCv(CvGenerationRequestDTO req) throws TikaException {
        String baseText;

        if (req.isUseUploadedCv() && req.getUploadedCv() != null && !req.getUploadedCv().isEmpty()) {
            baseText = parseUploadedCv(req.getUploadedCv());
        } else {
            baseText = buildUserProfileText(req.getUserId());
        }

        String prompt =
        	    """
        	    Act as an expert Canadian resume writer. Create a tailored, ATS-friendly Canadian-style resume 
        	    (1–2 pages, plain text, no tables, no columns, no graphics).

        	    VERY IMPORTANT RULES:
        	    - Do NOT add or invent any skills, experiences, degrees, or certifications not in USER DATA.
        	    - Only reorganize, rephrase, or emphasize existing information to match the job description.
        	    - If something is missing compared to the job description, simply omit it — never fabricate.
        	    - Follow Canadian standards (no photo, no personal details like birthdate or marital status).

        	    FORMAT (plain text, ATS-safe):
        	    1. Header: Name | City, Province | Phone | Email Address
        	    2. Professional Summary: 3–4 lines tailored to the job, truthfully based on USER DATA
        	    3. Experience:
        	       - Job Title — Company, City, Province (Start Date – End Date or Present)
        	         • 3–6 bullet points highlighting achievements and responsibilities
        	    4. Skills: Comma-separated list of skills from USER DATA only
        	    
        	    5. Education:
        	       - Degree, University, City, Province (Start – End Date)

        	    LANGUAGE & STYLE:
        	    - Professional, concise, achievement-focused.
        	    - Use strong action verbs, measurable results, and keywords from the job description (only if they apply).
        	    - Keep formatting simple: clear section headers, plain text bullets.

        	    USER DATA:
        	    %s

        	    JOB DESCRIPTION:
        	    %s
        	    """.formatted(baseText, req.getJobDescription());



        String generated = generateText(prompt);

        CvGenerationResponseDTO out = new CvGenerationResponseDTO();
        out.setGeneratedCvText(generated);
        return out;
    }

    private String parseUploadedCv(MultipartFile cv) throws TikaException {
        try {
            Tika tika = new Tika();
            return tika.parseToString(cv.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse uploaded CV", e);
        }
    }

    // Simple PDF text layout (ATS-safe). For nicer layout, replace with iText/thymeleaf->html->pdf pipeline.
    public byte[] exportCvToPdf(String cvText) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Resume");
                cs.endText();

                y -= 30; // spacing

                cs.setFont(PDType1Font.HELVETICA, 11);
                for (String line : cvText.split("\n")) {
                    if (line.trim().isEmpty()) {
                        y -= 12; // add spacing for empty lines
                        continue;
                    }

                    if (line.endsWith(":")) {
                        // section header
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    } else {
                        cs.setFont(PDType1Font.HELVETICA, 11);
                    }

                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(line.replace("•", "\u2022")); // ensure proper bullet char
                    cs.endText();
                    y -= 14;

                    if (y < 70f) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }
    
    
    //export as docx

    public byte[] exportCvToWord(String cvText) {
        try (XWPFDocument doc = new XWPFDocument()) {

            // Create bullet numbering style
            BigInteger bulletNumId = createBulletNumbering(doc);

            String[] lines = cvText.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                if (line.endsWith(":")) {
                    // Section header
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun run = p.createRun();
                    run.setBold(true);
                    run.setFontSize(12);
                    run.setText(line.trim());
                } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("\t")) {
                    // Bullet point
                    XWPFParagraph p = doc.createParagraph();
                    p.setNumID(bulletNumId); // apply numbering style
                    XWPFRun run = p.createRun();
                    run.setFontSize(11);
                    run.setText(line.replaceFirst("[-•\\t]\\s*", "").trim());
                } else {
                    // Normal paragraph
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun run = p.createRun();
                    run.setFontSize(11);
                    run.setText(line.trim());
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Word document", e);
        }
    }

    private BigInteger createBulletNumbering(XWPFDocument doc) {
        XWPFNumbering numbering = doc.createNumbering();

        CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
        abstractNum.setAbstractNumId(BigInteger.ZERO);

        // Create bullet level
        CTLvl level = abstractNum.addNewLvl();
        level.setIlvl(BigInteger.ZERO);
        level.addNewNumFmt().setVal(STNumberFormat.BULLET);
        level.addNewLvlText().setVal("•");
        level.addNewStart().setVal(BigInteger.ONE);

        XWPFAbstractNum abs = new XWPFAbstractNum(abstractNum);
        BigInteger abstractId = numbering.addAbstractNum(abs);
        return numbering.addNum(abstractId);
    }


   /* public byte[] exportCvToPdf(String cvText) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;
            float width = page.getMediaBox().getWidth() - 2 * margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.beginText();
                cs.newLineAtOffset(margin, y);

                for (String line : wrapText(cvText, width, 11)) {
                    if (y < 70f) { // new page
                        cs.endText();
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        PDPageContentStream cs2 = new PDPageContentStream(doc, page);
                        cs2.setFont(PDType1Font.HELVETICA, 11);
                        cs2.beginText();
                        cs2.newLineAtOffset(margin, y);
                        // swap handle
                        return exportCvToPdf(cvText); // fallback simple approach; for brevity not fully streaming
                    }
                    cs.showText(line);
                    cs.newLineAtOffset(0, -14f);
                    y -= 14f;
                }
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }*/

    // naive text wrapper by width in points
    private List<String> wrapText(String text, float maxWidth, int fontSize) throws IOException {
        // For brevity, keep it basic: split on newlines; real implementation would measure string width.
        return List.of(text.split("\n"));
    }
    
    //together AI service
    public String generateText(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("messages", new Object[]{
                    Map.of("role", "system", "content", "You are an expert resume writer."),
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
    
 //user detail service
 // fetches profile, skills, experience, education
    public String buildUserProfileText(String userId) {
        var user = userRepository.findById(userId).orElseThrow();
        var skills = userSkillRepository.findByUserId(userId)
                .stream().map(s -> s.getSkillName()).collect(Collectors.joining(", "));
        var exps = userExperienceRepository.findByUserId(userId);
        var edus = userEducationRepository.findByUserId(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(user.getUserName()).append("\n");
        sb.append("Email: ").append(user.getEmailAddress()).append("\n");
        //sb.append("Phone: ").append(user.getPhone()).append("\n");
        sb.append("Summary: ").append(user.getSummary() == null ? "" : user.getSummary()).append("\n");
        sb.append("Skills: ").append(skills).append("\n\n");

        sb.append("Experience:\n");
        for (var e : exps) {
            sb.append("- ").append(e.getJobTitle()).append(" — ").append(e.getCompanyName())
              .append(" (").append(e.getStartDate()).append(" - ").append(e.getEndDate() == null ? "Present" : e.getEndDate()).append(")\n");
            if (e.getJobDescription() != null) sb.append("  ").append(e.getJobDescription()).append("\n");
        }
        sb.append("\nEducation:\n");
        for (var ed : edus) {
            sb.append("- ").append(ed.getDegreeType()).append(", ").append(ed.getUniversityName())
              .append(" (").append(ed.getStartDate()).append(" - ").append(ed.getEndDate()).append(")\n");
        }
        return sb.toString();
    }
}
