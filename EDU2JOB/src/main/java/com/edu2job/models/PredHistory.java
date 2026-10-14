package com.edu2job.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One archived prediction: the nine input features that were sent to the ML
 * service, the raw model response, and the model version that produced it.
 *
 * <p>Rows written by the old (v1) model have {@code model_version = NULL}
 * and null values in the new-schema columns. They are preserved untouched so
 * old-model and new-model predictions remain distinguishable.</p>
 */
@Entity
@Table(name = "pred_history")
public class PredHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String major;

    @Column(columnDefinition = "numeric(4,2)")
    private BigDecimal cgpa;

    private String degree;

    @Column(columnDefinition = "TEXT")
    private String skills;

    /** Legacy column from the old model schema. No longer written. */
    @Column(name = "year_of_graduation")
    private Integer yearOfGraduation;

    // ---- EDU2JOB 2.0 nine-feature schema additions (all nullable) ----

    @Column(name = "certification_name")
    private String certificationName;

    @Column(name = "internship_experience")
    private String internshipExperience;

    @Column(name = "projects_count")
    private Integer projectsCount;

    @Column(name = "communication_skill")
    private String communicationSkill;

    @Column(name = "experience_months")
    private Integer experienceMonths;

    /** Version reported by the ML service, e.g. {@code edu2job-v2}. */
    @Column(name = "model_version")
    private String modelVersion;

    @Column(columnDefinition = "TEXT")
    private String predictedOutput;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public Integer getYearOfGraduation() { return yearOfGraduation; }
    public void setYearOfGraduation(Integer yearOfGraduation) { this.yearOfGraduation = yearOfGraduation; }
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
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getPredictedOutput() { return predictedOutput; }
    public void setPredictedOutput(String predictedOutput) { this.predictedOutput = predictedOutput; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
