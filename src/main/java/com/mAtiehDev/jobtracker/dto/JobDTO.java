package com.mAtiehDev.jobtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class JobDTO {
    private String title;
    private String company;
    private String location;
    private String snippet;

    @JsonProperty("link") 
    private String url;
    private String salary;
}
