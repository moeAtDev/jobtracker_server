package com.mAtiehDev.jobtracker.controller;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mAtiehDev.jobtracker.dto.JobApplicationDTO;
import com.mAtiehDev.jobtracker.model.TrackedApplication;
import com.mAtiehDev.jobtracker.service.GmailService;
import com.mAtiehDev.jobtracker.service.JwtService;


@RestController
@RequestMapping("/gmail")
public class GmailController {

    @Autowired 
    private GmailService gmailService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/apps")
    public ResponseEntity<?> getApplications(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        try {
            List<TrackedApplication> apps = gmailService.loadOrSyncApps(userId);
            return ResponseEntity.ok(apps);
        } catch (RuntimeException ex) {
            if ("AUTH_REQUIRED".equals(ex.getMessage())) {
                String authUrl = gmailService.buildAuthUrl(userId);
                return ResponseEntity.status(401).body(Map.of("authUrl", authUrl));
            }
            throw ex;
        }
    }
    
    @PutMapping("/apps/bulk-update")
    public ResponseEntity<?> updateApps(
            @RequestBody List<JobApplicationDTO> updatedApps,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        gmailService.updateApplications(updatedApps, userId);

        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/apps")
    public ResponseEntity<?> deleteApplications(@RequestBody Map<String, List<Long>> body,
                                                @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token); // adapt to your JWT service

        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("No IDs provided");
        }

        gmailService.deleteApplicationsByIdsAndUser(ids, userId);

        return ResponseEntity.ok().build();
    }


    
    
    @GetMapping("/refresh")
    public ResponseEntity<List<TrackedApplication>> refreshApplications(@RequestHeader("Authorization") String authHeader) {
    	
    	String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        System.out.println("user id  "+userId );
        try {
            List<TrackedApplication> refreshedApps = gmailService.refreshApplications(userId);
            return ResponseEntity.ok(refreshedApps);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    @PostMapping("/auth/callback")
    public ResponseEntity<Void> saveAuthToken(@RequestParam String code, @RequestParam String state) {
        gmailService.saveAuthCode(state, code); // state = userId
        return ResponseEntity.ok().build();
    }
    
    //application details endPoints 
    
    @GetMapping("/apps/{id}")
    public ResponseEntity<TrackedApplication> getApplicationById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        TrackedApplication app = gmailService.getApplicationById(id, userId);
        if (app == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(app);
    }

    @PutMapping("/apps/{id}")
    public ResponseEntity<TrackedApplication> updateApplication(
            @PathVariable Long id,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("company") String company,
            @RequestParam("source") String source,
            @RequestParam("appliedAt") String appliedAtStr,
            @RequestParam("emailSnippet") String emailSnippet,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestParam(value = "resumeText", required = false) String resumeText,
            @RequestHeader("Authorization") String authHeader) throws Exception {

        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        Instant appliedAt = null;
        if (appliedAtStr != null && !appliedAtStr.isEmpty()) {
            appliedAt = Instant.parse(appliedAtStr);
        }

        if (resumeFile != null && !resumeFile.isEmpty()) {
            try (InputStream is = resumeFile.getInputStream()) {
                Parser parser = new AutoDetectParser();
                BodyContentHandler handler = new BodyContentHandler(-1);
                Metadata metadata = new Metadata();
                ParseContext context = new ParseContext();
                parser.parse(is, handler, metadata, context);
                resumeText = handler.toString();
            }
        }

        TrackedApplication updatedApp = new TrackedApplication();
        updatedApp.setJobTitle(jobTitle);
        updatedApp.setCompany(company);
        updatedApp.setSource(source);
        updatedApp.setAppliedAt(appliedAt);
        updatedApp.setEmailSnippet(emailSnippet);
        updatedApp.setJobDescription(jobDescription);
        updatedApp.setResumeText(resumeText);

        if (resumeFile != null && !resumeFile.isEmpty()) {
            updatedApp.setResumeFileData(resumeFile.getBytes());
            updatedApp.setResumeFileName(resumeFile.getOriginalFilename());
            updatedApp.setResumeFileType(resumeFile.getContentType());
        }

        gmailService.updateSingleApplication(id, updatedApp, userId);

        TrackedApplication savedApp = gmailService.getApplicationById(id, userId);

        return ResponseEntity.ok(savedApp);
    }




    @DeleteMapping("/apps/{id}")
    public ResponseEntity<?> deleteApplication(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        gmailService.deleteSingleApplication(id, userId);
        return ResponseEntity.ok().build();
    }
    
    //download endpoint
    
    @GetMapping("/apps/{id}/resume")
    public ResponseEntity<byte[]> downloadResume(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);

        TrackedApplication app = gmailService.getApplicationById(id, userId);
        if (app == null || app.getResumeFileData() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + app.getResumeFileName() + "\"")
            .contentType(MediaType.parseMediaType(app.getResumeFileType()))
            .body(app.getResumeFileData());
    }


}
