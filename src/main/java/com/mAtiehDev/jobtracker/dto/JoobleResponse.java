package com.mAtiehDev.jobtracker.dto;

import lombok.Data;
import java.util.List;

@Data
public class JoobleResponse {
    private List<JobDTO> jobs;
}
