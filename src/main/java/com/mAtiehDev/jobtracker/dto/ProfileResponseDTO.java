package com.mAtiehDev.jobtracker.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProfileResponseDTO {
    private PersonalDetailsDTO personalDetails;
    private List<ExperienceDTO> experiences;
    
    private List<EducationDTO> education;

    private List<SkillDTO> skill;
    
    public List<ExperienceDTO> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<ExperienceDTO> experiences) {
        this.experiences = experiences;
    }
    
    public List<EducationDTO> getEducation() {
        return education;
    }
    
    public void setEducation(List<EducationDTO> education) {
        this.education = education;
    }

    public List<SkillDTO> getskill() {
        return skill;
    }
    
    public void setSkill(List<SkillDTO> skill) {
        this.skill = skill;
    }

    
    
    // Later: add other sections
    // private List<EducationDTO> education;
    // private List<ExperienceDTO> experience;
    // private List<SkillDTO> skills;
}
