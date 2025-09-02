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
        // Unified pipeline for uploaded or user data
        String baseText = getUnifiedCvText(req);
        baseText = enhanceProfileWithAi(baseText, req.getJobDescription());

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
        generated = normalizeCvText(generated);

        CvGenerationResponseDTO out = new CvGenerationResponseDTO();
        out.setGeneratedCvText(generated);
        return out;
    }

    private String getUnifiedCvText(CvGenerationRequestDTO req) throws TikaException {
        String rawText;
        if (req.isUseUploadedCv() && req.getUploadedCv() != null && !req.getUploadedCv().isEmpty()) {
            rawText = parseUploadedCv(req.getUploadedCv());
        } else {
            rawText = buildUserProfileText(req.getUserId());
        }
        // normalize headers and bullets for both sources
        return normalizeCvText(rawText);
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
    public byte[] exportCvToPdf(String cvText) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            float margin = 50f;
            float lineHeight = 14f;
            float sectionSpacing = 20f;
            float wrapWidth = PDRectangle.LETTER.getWidth() - 2 * margin;

            boolean inEducationSection = false;
            boolean inSkillsSection = false;
            boolean inExperienceSection = false;
            boolean expectSchoolLine = false;

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - margin;

            String[] lines = cvText.split("\\r?\\n");

            for (int i = 0; i < lines.length; i++) {
                String rawLine = lines[i].trim();
                if (rawLine.isEmpty()) {
                    y -= lineHeight;
                    continue;
                }

                // Start new page if needed
                if (y < 70f) {
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {

                    // ----- Section headers -----
                    if (rawLine.equalsIgnoreCase("PROFILE") ||
                        rawLine.equalsIgnoreCase("EXPERIENCE") ||
                        rawLine.equalsIgnoreCase("EDUCATION") ||
                        rawLine.equalsIgnoreCase("SKILLS") ||
                        rawLine.equalsIgnoreCase("REFERENCES"))
                    {
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                        y -= sectionSpacing;

                        cs.beginText();
                        cs.newLineAtOffset(margin, y);
                        cs.showText(rawLine.toUpperCase());
                        cs.endText();

                        y -= 4;
                        cs.moveTo(margin, y);
                        cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                        cs.stroke();
                        y -= lineHeight;

                        inEducationSection = rawLine.equalsIgnoreCase("EDUCATION");
                        inSkillsSection = rawLine.equalsIgnoreCase("SKILLS");
                        inExperienceSection = rawLine.equalsIgnoreCase("EXPERIENCE");
                        expectSchoolLine = false;
                        continue;
                    }

                    float indent = margin;
                    PDType1Font font = PDType1Font.HELVETICA;
                    int fontSize = 11;

                    // ----- Name & Contact -----
                    if (i == 0) {
                        // Name: centered
                        font = PDType1Font.HELVETICA_BOLD;
                        fontSize = 16;
                        float textWidth = font.getStringWidth(rawLine) / 1000 * fontSize;
                        float startX = (PDRectangle.LETTER.getWidth() - textWidth) / 2;
                        cs.setFont(font, fontSize);
                        cs.beginText();
                        cs.newLineAtOffset(startX, y);
                        cs.showText(rawLine);
                        cs.endText();
                        y -= lineHeight + 4;
                        continue;
                    } else if (i == 1) {
                        // Contact info: split by "|" and print each part on a new line
                        font = PDType1Font.HELVETICA;
                        fontSize = 11;
                        cs.setFont(font, fontSize);
                        String[] contactParts = rawLine.split("\\|");
                        for (String part : contactParts) {
                            String trimmed = part.trim();
                            cs.beginText();
                            cs.newLineAtOffset(margin, y);
                            cs.showText(trimmed);
                            cs.endText();
                            y -= lineHeight;
                        }
                        continue;
                    }

                    // ----- Education -----
                    if (inEducationSection) {
                        indent += 20;
                        font = expectSchoolLine ? PDType1Font.HELVETICA : PDType1Font.HELVETICA_BOLD;
                        expectSchoolLine = !expectSchoolLine;
                    }

                    // ----- Skills -----
                    if (inSkillsSection) {
                        indent += 20;
                        font = PDType1Font.HELVETICA;
                    }

                    // ----- Experience -----
                    if (inExperienceSection) {
                        if (rawLine.toUpperCase().startsWith("JOBTITLE:")) {
                            font = PDType1Font.HELVETICA_BOLD;
                            rawLine = rawLine.substring("JOBTITLE:".length()).trim();
                        } else if (rawLine.toUpperCase().startsWith("COMPANY:")) {
                            font = PDType1Font.HELVETICA_BOLD;
                            rawLine = rawLine.substring("COMPANY:".length()).trim();
                        } else if (rawLine.toUpperCase().startsWith("DATES:")) {
                            font = PDType1Font.HELVETICA_OBLIQUE;
                            rawLine = rawLine.substring("DATES:".length()).trim();
                        } else if (rawLine.startsWith("•") || rawLine.startsWith("-") || rawLine.startsWith("*")) {
                            font = PDType1Font.HELVETICA;
                            rawLine = "• " + rawLine.replaceFirst("^[\\-*•]\\s*", "");
                            indent += 10;
                        }
                    }

                    cs.setFont(font, fontSize);
                    y = drawWrappedText(cs, rawLine, indent, y, wrapWidth - (indent - margin), lineHeight);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }


    // draw wrapped text line by line, returns updated Y
    private float drawWrappedText(PDPageContentStream cs, String text, float startX, float startY, float maxWidth, float lineHeight) throws IOException {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float y = startY;
        for (String word : words) {
            String tempLine = line.length() == 0 ? word : line + " " + word;
            float textWidth = PDType1Font.HELVETICA.getStringWidth(tempLine) / 1000 * 11;
            if (textWidth > maxWidth) {
                cs.beginText();
                cs.newLineAtOffset(startX, y);
                cs.showText(line.toString());
                cs.endText();
                line = new StringBuilder(word);
                y -= lineHeight;
            } else {
                line = new StringBuilder(tempLine);
            }
        }
        if (line.length() > 0) {
            cs.beginText();
            cs.newLineAtOffset(startX, y);
            cs.showText(line.toString());
            cs.endText();
            y -= lineHeight;
        }
        return y;
    }



    
    
    //export as docx
    public byte[] exportCvToWord(String cvText) {
        try (XWPFDocument doc = new XWPFDocument()) {
            BigInteger bulletNumId = createBulletNumbering(doc);
            String[] lines = cvText.split("\\r?\\n");

            boolean inEducationSection = false;
            boolean expectSchoolLine = false;
            boolean inSkillsSection = false;

            for (int i = 0; i < lines.length; i++) {
                String raw = lines[i];
                String line = raw == null ? "" : raw.trim();
                if (line.isEmpty()) continue;

                // ----- Section headers detection -----
                if (line.equalsIgnoreCase("PROFILE") ||
                    line.equalsIgnoreCase("WORK EXPERIENCE") ||
                    line.equalsIgnoreCase("EDUCATION") ||
                    line.equalsIgnoreCase("SKILLS")) 
                {
                    XWPFParagraph headerPara = doc.createParagraph();
                    headerPara.setSpacingBefore(200);
                    // Add bottom border for separator
                    headerPara.setBorderBottom(Borders.SINGLE);

                    XWPFRun headerRun = headerPara.createRun();
                    headerRun.setBold(true);
                    headerRun.setFontSize(12);
                    headerRun.setFontFamily("Calibri");
                    headerRun.setText(line.toUpperCase());

                    // track education + skills
                    inEducationSection = line.equalsIgnoreCase("EDUCATION");
                    inSkillsSection = line.equalsIgnoreCase("SKILLS");
                    expectSchoolLine = false;
                    continue;
                }

                // ----- EDUCATION logic -----
                if (inEducationSection) {
                    if (!expectSchoolLine) {
                        XWPFParagraph degPara = doc.createParagraph();
                        degPara.setIndentationLeft(360);
                        degPara.setSpacingBefore(80);
                        XWPFRun degRun = degPara.createRun();
                        degRun.setBold(true);
                        degRun.setFontSize(11);
                        degRun.setFontFamily("Calibri");
                        degRun.setText(line);
                        expectSchoolLine = true;
                    } else {
                        XWPFParagraph schoolPara = doc.createParagraph();
                        schoolPara.setIndentationLeft(360);
                        schoolPara.setSpacingAfter(120);
                        XWPFRun schoolRun = schoolPara.createRun();
                        schoolRun.setFontSize(11);
                        schoolRun.setFontFamily("Calibri");
                        schoolRun.setText(line);

                        XWPFParagraph gap = doc.createParagraph();
                        gap.createRun().addBreak();

                        expectSchoolLine = false;
                    }
                    continue;
                }

                // ----- SKILLS logic -----
                if (inSkillsSection) {
                    XWPFParagraph skillPara = doc.createParagraph();
                    skillPara.setIndentationLeft(360);
                    skillPara.setSpacingBefore(80);
                    XWPFRun skillRun = skillPara.createRun();
                    skillRun.setFontSize(11);
                    skillRun.setFontFamily("Calibri");
                    skillRun.setText(line);
                    continue;
                }

                // ----- Non-education/skills -----
                if (i == 0) {
                    XWPFParagraph namePara = doc.createParagraph();
                    namePara.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun nameRun = namePara.createRun();
                    nameRun.setBold(true);
                    nameRun.setFontFamily("Calibri");
                    nameRun.setFontSize(16);
                    nameRun.setText(line);
                    continue;
                }

                if (i == 1) {
                    XWPFParagraph contactPara = doc.createParagraph();
                    contactPara.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun contactRun = contactPara.createRun();
                    contactRun.setFontFamily("Calibri");
                    contactRun.setFontSize(11);
                    contactRun.setText(line);
                    continue;
                }

                XWPFParagraph p = doc.createParagraph();
                p.setSpacingBefore(100);
                XWPFRun run = p.createRun();
                run.setFontFamily("Calibri");
                run.setFontSize(11);

                if (line.toUpperCase().startsWith("JOBTITLE:")) {
                    run.setBold(true);
                    run.setText(line.substring("JOBTITLE:".length()).trim());
                    continue;
                }
                if (line.toUpperCase().startsWith("COMPANY:")) {
                    run.setBold(true);
                    run.setText(line.substring("COMPANY:".length()).trim());
                    continue;
                }
                if (line.toUpperCase().startsWith("DATES:")) {
                    run.setItalic(true);
                    run.setText(line.substring("DATES:".length()).trim());
                    continue;
                }

                if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                    p.setNumID(bulletNumId);
                    p.setIndentationLeft(360);
                    String content = line.replaceFirst("^[\\-*•]\\s*", "").trim();
                    run.setText(content);
                    continue;
                }

                run.setText(line);
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


   

    private String normalizeCvText(String raw) {
        if (raw == null) return "";

        return raw.replaceAll("(?i)^profile:?$", "PROFILE")
                  .replaceAll("(?i)^professional summary:?$", "PROFILE")
                  .replaceAll("(?i)^work experience:?$", "WORK EXPERIENCE")
                  .replaceAll("(?i)^experience:?$", "WORK EXPERIENCE")
                  .replaceAll("(?i)^education:?$", "EDUCATION")
                  .replaceAll("(?i)^skills:?$", "SKILLS")
                  .replaceAll("(?i)^references:?$", "REFERENCES")
                  // standardize bullets for consistent Word/PDF export
                  .replaceAll("^[\\-*•]\\s*", "• ");
    }

    private String enhanceProfileWithAi(String cvText, String jobDescription) {
        String profile = cvText.split("(?i)EXPERIENCE")[0]; // grab PROFILE section
        String prompt = """
            Act as an expert resume writer. Rewrite this PROFILE section to be concise, professional,
            and tailored to the job description. Keep all info factual.
            PROFILE:
            %s
            JOB DESCRIPTION:
            %s
            """.formatted(profile, jobDescription);

        String enhanced = generateText(prompt);
        // replace PROFILE section in cvText
        return cvText.replaceFirst("(?i)^PROFILE.*?EXPERIENCE", enhanced + "\n\nEXPERIENCE");
    }


    
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
        var exps = userExperienceRepository.findByUserId(userId);
        var edus = userEducationRepository.findByUserId(userId);
        var skills = userSkillRepository.findByUserId(userId)
                .stream().map(s -> s.getSkillName()).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();

        // Name + Contact Info
        sb.append(user.getUserName()).append("\n");
        sb.append(user.getUserCity()).append(" | ")
         // .append(user.getPhoneNumber() == null ? "" : user.getPhoneNumber()).append(" | ")
          .append(user.getEmailAddress()).append("\n\n");

        // Profile / Summary
        sb.append("PROFILE").append("\n");
        sb.append(user.getSummary() == null ? "" : user.getSummary()).append("\n\n");

        // Experience
        sb.append("WORK EXPERIENCE").append("\n");
        for (var e : exps) {
            sb.append("JOBTITLE:").append(e.getJobTitle()).append("\n");  // Mark job title
            sb.append("COMPANY:").append(e.getCompanyName()).append("\n"); // Mark company
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

        // Education
        sb.append("EDUCATION").append("\n");
        for (var ed : edus) {
            sb.append("DEGREE:").append(ed.getDegreeType()).append("\n");
            sb.append("SCHOOL:").append(ed.getUniversityName())
              .append(", ").append(ed.getCity())
              .append(" | ").append(ed.getEndDate()).append("\n\n");
        }

        // Skills
        sb.append("SKILLS").append("\n");
        sb.append("Technical Skills:\n");
        sb.append(String.join(", ", skills)).append("\n");
        sb.append("Soft Skills:\n");
        sb.append("Team Work, Communication skills, Time Management, Problem Solving\n"); // You can fetch soft skills from DB too

        return sb.toString();
    }


}
