package com.edu2job.dto;

import com.edu2job.models.UserPredictionHistory;
import com.edu2job.util.SkillsJson;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The authenticated user's current profile, in the shared snake_case wire
 * format used by React, Spring Boot and Flask.
 */
public class ProfileResponse {

    private String username;
    private String degree;
    private String major;
    private BigDecimal cgpa;
    private List<String> skills;

    @JsonProperty("certification_name")
    private String certificationName;

    @JsonProperty("internship_experience")
    private String internshipExperience;

    @JsonProperty("projects_count")
    private Integer projectsCount;

    @JsonProperty("communication_skill")
    private String communicationSkill;

    @JsonProperty("experience_months")
    private Integer experienceMonths;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /** Build a response from a persisted profile row owned by {@code username}. */
    public static ProfileResponse from(UserPredictionHistory profile, String username) {
        ProfileResponse response = new ProfileResponse();
        response.username = username;
        response.degree = profile.getDegree();
        response.major = profile.getMajor();
        response.cgpa = profile.getCgpa();
        response.skills = SkillsJson.decode(profile.getSkills());
        response.certificationName = profile.getCertificationName();
        response.internshipExperience = profile.getInternshipExperience();
        response.projectsCount = profile.getProjectsCount();
        response.communicationSkill = profile.getCommunicationSkill();
        response.experienceMonths = profile.getExperienceMonths();
        response.updatedAt = profile.getCreatedAt();
        return response;
    }

    /** Number of the nine model fields that currently hold a value (0–9). */
    public int completedFieldCount() {
        int count = 0;
        if (degree != null && !degree.isBlank()) count++;
        if (major != null && !major.isBlank()) count++;
        if (cgpa != null) count++;
        if (skills != null && !skills.isEmpty()) count++;
        if (certificationName != null && !certificationName.isBlank()) count++;
        if (internshipExperience != null && !internshipExperience.isBlank()) count++;
        if (projectsCount != null) count++;
        if (communicationSkill != null && !communicationSkill.isBlank()) count++;
        if (experienceMonths != null) count++;
        return count;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
