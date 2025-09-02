package com.mAtiehDev.jobtracker.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CoverLetterRequestDTO {
    private boolean useUploadedCv;
    private MultipartFile uploadedCv;
    private String userId;
    private String jobDescription;
}
