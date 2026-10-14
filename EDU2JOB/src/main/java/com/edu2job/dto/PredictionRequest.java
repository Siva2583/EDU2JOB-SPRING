package com.edu2job.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prediction payload carrying exactly the nine input features of the new
 * EDU2JOB 2.0 model. This object is validated in Spring Boot and then
 * forwarded, field for field, to the Flask ML service.
 */
public class PredictionRequest {

    @NotBlank(message = "degree is required")
    @Size(max = 100, message = "degree must be at most 100 characters")
    private String degree;

    @NotBlank(message = "major is required")
    @Size(max = 150, message = "major must be at most 150 characters")
    private String major;

    @NotNull(message = "cgpa is required")
    @DecimalMin(value = "0.0", message = "cgpa must be at least 0")
    @DecimalMax(value = "10.0", message = "cgpa must be at most 10 (10-point scale)")
    private BigDecimal cgpa;

    @NotEmpty(message = "at least one skill is required")
    private List<@NotBlank(message = "skills must not contain blank entries")
                 @Size(max = 100, message = "each skill must be at most 100 characters") String> skills;

    /** Optional. {@code null} means the student has no certification. */
    @JsonAlias("certificationName")
    @Size(max = 255, message = "certification_name must be at most 255 characters")
    private String certificationName;

    @NotBlank(message = "internship_experience is required")
    @JsonAlias("internshipExperience")
    @Size(max = 50, message = "internship_experience must be at most 50 characters")
    private String internshipExperience;

    @NotNull(message = "projects_count is required")
    @JsonAlias("projectsCount")
    @Min(value = 0, message = "projects_count must be a non-negative integer")
    private Integer projectsCount;

    @NotBlank(message = "communication_skill is required")
    @JsonAlias("communicationSkill")
    @Size(max = 50, message = "communication_skill must be at most 50 characters")
    private String communicationSkill;

    @NotNull(message = "experience_months is required")
    @JsonAlias("experienceMonths")
    @Min(value = 0, message = "experience_months must be a non-negative integer")
    private Integer experienceMonths;

    /**
     * The exact JSON body sent to Flask {@code POST /predict}. Keys are the
     * snake_case contract names, in schema order.
     */
    public Map<String, Object> toFlaskPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("degree", degree);
        payload.put("major", major);
        payload.put("cgpa", cgpa);
        payload.put("skills", skills);
        payload.put("certification_name", certificationName);
        payload.put("internship_experience", internshipExperience);
        payload.put("projects_count", projectsCount);
        payload.put("communication_skill", communicationSkill);
        payload.put("experience_months", experienceMonths);
        return payload;
    }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String certificationName) { this.certificationName = certificationName; }
    public String getInternshipExperience() { return internshipExperience; }
    public void setInternshipExperience(String internshipExperience) { this.internshipExperience = internshipExperience; }
    public Integer getProjectsCount() { return projectsCount; }
    public void setProjectsCount(Integer projectsCount) { this.projectsCount = projectsCount; }
    public String getCommunicationSkill() { return communicationSkill; }
    public void setCommunicationSkill(String communicationSkill) { this.communicationSkill = communicationSkill; }
    public Integer getExperienceMonths() { return experienceMonths; }
    public void setExperienceMonths(Integer experienceMonths) { this.experienceMonths = experienceMonths; }
}
