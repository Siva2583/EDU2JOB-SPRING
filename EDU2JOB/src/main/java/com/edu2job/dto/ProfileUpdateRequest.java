package com.edu2job.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partial-update payload for the EDU2JOB 2.0 nine-field student profile.
 *
 * <p>Every field is optional: a field that is absent (or JSON {@code null})
 * is left unchanged on the stored profile. A field that is present — including
 * an empty {@code skills} list — replaces the stored value. This preserves the
 * partial-update behaviour of the previous profile endpoint while moving to
 * the new model schema:
 * {@code degree, major, cgpa, skills, certification_name,
 * internship_experience, projects_count, communication_skill,
 * experience_months}.</p>
 *
 * <p>JSON names are the snake_case names of the shared API contract; the
 * camelCase field names are also accepted as aliases for convenience.</p>
 */
public class ProfileUpdateRequest {

    @Size(max = 100, message = "degree must be at most 100 characters")
    private String degree;

    @Size(max = 150, message = "major must be at most 150 characters")
    private String major;

    @DecimalMin(value = "0.0", message = "cgpa must be at least 0")
    @DecimalMax(value = "10.0", message = "cgpa must be at most 10 (10-point scale)")
    private BigDecimal cgpa;

    /** Multiple skills. An empty list explicitly clears the stored skills. */
    private List<@Size(max = 100, message = "each skill must be at most 100 characters") String> skills;

    @JsonAlias("certificationName")
    @Size(max = 255, message = "certification_name must be at most 255 characters")
    private String certificationName;

    @JsonAlias("internshipExperience")
    @Size(max = 50, message = "internship_experience must be at most 50 characters")
    private String internshipExperience;

    @JsonAlias("projectsCount")
    @Min(value = 0, message = "projects_count must be a non-negative integer")
    private Integer projectsCount;

    @JsonAlias("communicationSkill")
    @Size(max = 50, message = "communication_skill must be at most 50 characters")
    private String communicationSkill;

    @JsonAlias("experienceMonths")
    @Min(value = 0, message = "experience_months must be a non-negative integer")
    private Integer experienceMonths;

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
