// src/main/java/com/mAtiehDev/jobtracker/controller/CvGeneratorController.java
package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.dto.CvGenerationRequestDTO;
import com.mAtiehDev.jobtracker.dto.CvGenerationResponseDTO;
import com.mAtiehDev.jobtracker.service.CvGeneratorService;
import com.mAtiehDev.jobtracker.service.JwtService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvGeneratorController {

    private final CvGeneratorService cvGeneratorService;
    private final JwtService jwtService;

    @PostMapping(value = "/generate", consumes = {"multipart/form-data"})
    public ResponseEntity<CvGenerationResponseDTO> generate(@ModelAttribute CvGenerationRequestDTO req,
                                                            @RequestHeader("Authorization") String authHeader) throws TikaException {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        req.setUserId(userId);
        CvGenerationResponseDTO out = cvGeneratorService.generateCv(req);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, String> body,
                                            @RequestHeader("Authorization") String authHeader) {
        String cvText = body.get("cvText");
        byte[] pdf = cvGeneratorService.exportCvToPdf(cvText);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=GeneratedCV.pdf")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }
    
    @PostMapping("/export-docx")
    public ResponseEntity<byte[]> exportDocx(@RequestBody Map<String, String> body,
                                             @RequestHeader("Authorization") String authHeader) {
        String cvText = body.get("cvText");
        byte[] docx = cvGeneratorService.exportCvToWord(cvText);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=GeneratedCV.docx")
                .header("Content-Type", 
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .body(docx);
    }



    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipart(MultipartException e) {
        return ResponseEntity.badRequest().body("Invalid upload: " + e.getMessage());
    }
}
