package com.mAtiehDev.jobtracker.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
//not used 
//to be deleted 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequestDTO {
    private MultipartFile resume;
    private String jobDescription;
}