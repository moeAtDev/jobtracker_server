package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.dto.CoverLetterRequestDTO;
import com.mAtiehDev.jobtracker.dto.CoverLetterResponseDTO;
import com.mAtiehDev.jobtracker.service.CoverLetterGeneratorService;
import com.mAtiehDev.jobtracker.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/cover-letter")
@RequiredArgsConstructor
public class CoverLetterController {

    private final CoverLetterGeneratorService service;
    private final JwtService jwtService;

    @PostMapping(value = "/generate", consumes = {"multipart/form-data"})
    public ResponseEntity<CoverLetterResponseDTO> generateCoverLetter(
            @ModelAttribute CoverLetterRequestDTO req,
            @RequestHeader("Authorization") String authHeader) throws TikaException {

        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.extractUserId(token);
        req.setUserId(userId);

        return ResponseEntity.ok(service.generateCoverLetter(req));
    }

    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, String> body,
                                            @RequestHeader("Authorization") String authHeader) throws IOException {
        String token = authHeader.replace("Bearer ", ""); // optional if you want to validate
        String userId = jwtService.extractUserId(token);  // optional if needed

        String letter = body.get("generatedCoverLetter");
        byte[] pdf = service.exportCoverLetterToPdf(letter);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=CoverLetter.pdf")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }

    @PostMapping("/export-docx")
    public ResponseEntity<byte[]> exportDocx(@RequestBody Map<String, String> body,
                                             @RequestHeader("Authorization") String authHeader) throws IOException {
        String token = authHeader.replace("Bearer ", ""); // optional if you want to validate
        String userId = jwtService.extractUserId(token);  // optional if needed

        String letter = body.get("generatedCoverLetter");
        byte[] docx = service.exportCoverLetterToWord(letter);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=CoverLetter.docx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .body(docx);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipart(MultipartException e) {
        return ResponseEntity.badRequest().body("Invalid upload: " + e.getMessage());
    }
}
