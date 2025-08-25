package com.mAtiehDev.jobtracker.service;

import com.mAtiehDev.jobtracker.dto.EducationDTO;
import com.mAtiehDev.jobtracker.dto.ExperienceDTO;
import com.mAtiehDev.jobtracker.dto.PersonalDetailsDTO;
import com.mAtiehDev.jobtracker.dto.ProfileResponseDTO;
import com.mAtiehDev.jobtracker.dto.SkillDTO;
import com.mAtiehDev.jobtracker.model.User;
import com.mAtiehDev.jobtracker.model.UserEducation;
import com.mAtiehDev.jobtracker.model.UserExperience;
import com.mAtiehDev.jobtracker.model.UserSkill;
import com.mAtiehDev.jobtracker.repository.UserEducationRepository;
import com.mAtiehDev.jobtracker.repository.UserExperienceRepository;
import com.mAtiehDev.jobtracker.repository.UserRepository;
import com.mAtiehDev.jobtracker.repository.UserSkillRepository;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  /*  @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserExperienceRepository experienceRepository;
    
    @Autowired
    private UserEducationRepository  educationRepository;
    
    @Autowired
    private UserSkillRepository  skillRepository;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    */
	
	  private final UserRepository userRepository;
	    private final UserExperienceRepository experienceRepository;
	    private final UserEducationRepository educationRepository;
	    private final UserSkillRepository skillRepository;
	    private final GlobalServices globalServices;

	    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	    @Autowired
	    public ProfileService(UserRepository userRepository,
	                          UserExperienceRepository experienceRepository,
	                          UserEducationRepository educationRepository,
	                          UserSkillRepository skillRepository,
	                          GlobalServices globalServices) {
	        this.userRepository = userRepository;
	        this.experienceRepository = experienceRepository;
	        this.educationRepository = educationRepository;
	        this.skillRepository = skillRepository;
	        this.globalServices = globalServices;
	    }

	    @Autowired
		private IdGenerator idGenerator;

    public ProfileResponseDTO getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Personal details
            PersonalDetailsDTO personalDetails = new PersonalDetailsDTO();
            personalDetails.setFirstName(user.getFirstName());
            personalDetails.setLastName(user.getLastName());
            personalDetails.setEmailAddress(user.getEmailAddress());
            personalDetails.setUserHeader(user.getUserHeader());
            personalDetails.setUserCity(user.getUserCity());
            personalDetails.setUserCountry(user.getUserCountry());
            personalDetails.setSummary(user.getSummary());

            // Experience section
            List<ExperienceDTO> experiences = experienceRepository.findByUserIdOrderByEndDateDesc(userId)
            	    .stream()
            	    .map(exp -> {
            	        ExperienceDTO dto = new ExperienceDTO();
            	        dto.setJobId(exp.getJobId());
            	        dto.setJobTitle(exp.getJobTitle());
            	        dto.setJobDescription(exp.getJobDescription());
            	        dto.setJobType(exp.getJobType());
            	        dto.setCompanyName(exp.getCompanyName());
            	        dto.setCompanyCity(exp.getCompanyCity());
            	        dto.setCompanyCountry(exp.getCompanyCountry());
            	        dto.setStartDate(exp.getStartDate() != null ? sdf.format(exp.getStartDate()) : null);
            	        dto.setEndDate(exp.getEndDate() != null ? sdf.format(exp.getEndDate()) : null);
            	        return dto;
            	    })
            	    .toList();

            
            //user education
            List<EducationDTO> education = educationRepository.findByUserIdOrderByEndDateDesc(userId)
                    .stream()
                    .map(edu -> {
                    	EducationDTO dto = new EducationDTO();
                        dto.setId(edu.getId()); // or adjust if still String
                        dto.setDegreeType(edu.getDegreeType());
                        dto.setMajor(edu.getMajor());
                        dto.setCountry(edu.getCountry());
                        dto.setCity(edu.getCity());
                        dto.setUniversityName(edu.getUniversityName());
                        dto.setStartDate(edu.getStartDate() != null ? sdf.format(edu.getStartDate()) : null);
                        dto.setEndDate(edu.getEndDate() != null ? sdf.format(edu.getEndDate()) : null);
                        return dto;
                    })
                    .toList();
            
            List<SkillDTO> skill = skillRepository.findByUserId(userId)
                    .stream()
                    .map(skl -> {
                    	SkillDTO dto = new SkillDTO();
                        dto.setId(skl.getId()); // or adjust if still String
                        dto.setSkillName(skl.getSkillName());
                        return dto;
                    })
                    .toList();
            
            

            // Final profile
            ProfileResponseDTO profile = new ProfileResponseDTO();
            profile.setPersonalDetails(personalDetails);
            profile.setExperiences(experiences);
            profile.setEducation(education);
            profile.setSkill(skill);

            return profile;
        }
    
    
    public void updatePersonalDetails(String userId, PersonalDetailsDTO detailsDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(detailsDTO.getFirstName());
        user.setLastName(detailsDTO.getLastName());
        user.setEmailAddress(detailsDTO.getEmailAddress());
        user.setUserHeader(detailsDTO.getUserHeader());
        user.setUserCity(detailsDTO.getUserCity());
        user.setUserCountry(detailsDTO.getUserCountry());
       // user.setSummary(detailsDTO.getSummary());

        userRepository.save(user); // saves updated fields
    }
    
    
    public void updateSummary(String userId, String summary) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setSummary(summary);
        userRepository.save(user);
    }
    
    

    public ExperienceDTO addExperience(String userId, ExperienceDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserExperience experience = new UserExperience();
        experience.setJobId(idGenerator.generateId("job_2025_", "experience_seq"));
        experience.setUserId(user.getUserId());
        experience.setUserName(user.getUserName());

        experience.setJobTitle(dto.getJobTitle());
        experience.setJobDescription(dto.getJobDescription());
        experience.setJobType(dto.getJobType());
        experience.setCompanyName(dto.getCompanyName());
        experience.setCompanyCity(dto.getCompanyCity());
        experience.setCompanyCountry(dto.getCompanyCountry());

        experience.setStartDate(globalServices.parseTimestamp(dto.getStartDate()));
        experience.setEndDate(globalServices.parseTimestamp(dto.getEndDate()));

        experienceRepository.save(experience);

        // Return DTO with generated ID
        dto.setJobId(experience.getJobId());
        return dto;
    }

    public void updateSingleExperience(String userId, ExperienceDTO dto) {
        if (dto.getJobId() == null || dto.getJobId().isEmpty()) {
            throw new IllegalArgumentException("Experience ID is required for update");
        }

        UserExperience experience = experienceRepository.findById(dto.getJobId())
                .orElseThrow(() -> new RuntimeException("Experience not found"));

        if (!experience.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this experience");
        }

        experience.setJobTitle(dto.getJobTitle());
        experience.setJobDescription(dto.getJobDescription());
        experience.setJobType(dto.getJobType());
        experience.setCompanyName(dto.getCompanyName());
        experience.setCompanyCity(dto.getCompanyCity());
        experience.setCompanyCountry(dto.getCompanyCountry());

        experience.setStartDate(globalServices.parseTimestamp(dto.getStartDate()));
        experience.setEndDate(globalServices.parseTimestamp(dto.getEndDate()));

        experienceRepository.save(experience);
    }

    
    public void deleteExperiences(String userId, List<String> experienceIds) {
        List<UserExperience> toDelete = experienceRepository.findAllById(experienceIds);

        for (UserExperience exp : toDelete) {
            if (exp.getUserId().equals(userId)) {
                experienceRepository.delete(exp);
            }
        }
    }

    
    public EducationDTO addNewEducation(String userId, EducationDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        UserEducation edu = new UserEducation();
        edu.setId(idGenerator.generateId("edu_2025_", "education_seq"));
        edu.setUserId(user.getUserId());
        edu.setUserName(user.getUserName());

        edu.setDegreeType(dto.getDegreeType());
        edu.setMajor(dto.getMajor());
        edu.setUniversityName(dto.getUniversityName());
        edu.setCountry(dto.getCountry());
        edu.setCity(dto.getCity());

        try {
            edu.setStartDate(dto.getStartDate() != null && !dto.getStartDate().isEmpty()
                    ? new Timestamp(sdf.parse(dto.getStartDate()).getTime()) : null);
            edu.setEndDate(dto.getEndDate() != null && !dto.getEndDate().isEmpty()
                    ? new Timestamp(sdf.parse(dto.getEndDate()).getTime()) : null);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid date format", e);
        }

        educationRepository.save(edu);

        // Return the saved entry with ID (to update frontend list)
        EducationDTO result = new EducationDTO();
        result.setId(edu.getId());
        result.setDegreeType(edu.getDegreeType());
        result.setMajor(edu.getMajor());
        result.setUniversityName(edu.getUniversityName());
        result.setCountry(edu.getCountry());
        result.setCity(edu.getCity());
        result.setStartDate(dto.getStartDate());
        result.setEndDate(dto.getEndDate());
        return result;
    }

    public void updateSingleEducation(String userId, EducationDTO dto) {
        if (dto.getId() == null || dto.getId().isEmpty()) {
            throw new IllegalArgumentException("Education ID is required for update");
        }

        UserEducation edu = educationRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Education entry not found"));

        if (!edu.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to edit this education entry");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        edu.setDegreeType(dto.getDegreeType());
        edu.setMajor(dto.getMajor());
        edu.setUniversityName(dto.getUniversityName());
        edu.setCountry(dto.getCountry());
        edu.setCity(dto.getCity());

        try {
            edu.setStartDate(dto.getStartDate() != null && !dto.getStartDate().isEmpty()
                    ? new Timestamp(sdf.parse(dto.getStartDate()).getTime()) : null);
            edu.setEndDate(dto.getEndDate() != null && !dto.getEndDate().isEmpty()
                    ? new Timestamp(sdf.parse(dto.getEndDate()).getTime()) : null);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid date format", e);
        }

        educationRepository.save(edu);
    }



    
    public void deleteEducation(String userId, String educationId) {
        UserEducation edu = educationRepository.findById(educationId)
                .orElseThrow(() -> new RuntimeException("Education entry not found"));

        if (!edu.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to education entry");
        }

        educationRepository.deleteById(educationId);
    }

    
    
    public UserSkill addSkill(String userId, SkillDTO dto) {
        if (skillRepository.existsByUserIdAndSkillName(userId, dto.getSkillName())) {
            throw new RuntimeException("Skill already exists");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        String userName = user.getFirstName() + " " + user.getLastName();

        UserSkill skill = new UserSkill();
        skill.setId(idGenerator.generateId("skill_2025_", "skill_seq"));
        skill.setUserId(userId);
        skill.setUserName(userName);
        skill.setSkillName(dto.getSkillName());

        return skillRepository.save(skill);
    }

    public void deleteSkill(String userId, String skillName) {
        UserSkill skill = skillRepository.findByUserIdAndSkillName(userId, skillName)
            .orElseThrow(() -> new RuntimeException("Skill not found"));
        skillRepository.delete(skill);
    }




    
    

    
}