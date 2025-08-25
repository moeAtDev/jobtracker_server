package com.mAtiehDev.jobtracker.dto;

import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

@Data
public class CvGenerationRequestDTO {
    private boolean useUploadedCv;      // true: parse uploaded CV; false: use DB profile data
    private String jobDescription;      // JD text
    private String userId;              // from JWT typically
    private MultipartFile uploadedCv;   // optional
}
