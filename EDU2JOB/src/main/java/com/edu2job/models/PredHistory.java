package com.edu2job.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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
    private Double cgpa;

    private String degree;

    @Column(columnDefinition = "TEXT")
    private String skills;

    private Integer yearOfGraduation;

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
    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public Integer getYearOfGraduation() { return yearOfGraduation; }
    public void setYearOfGraduation(Integer yearOfGraduation) { this.yearOfGraduation = yearOfGraduation; }
    public String getPredictedOutput() { return predictedOutput; }
    public void setPredictedOutput(String predictedOutput) { this.predictedOutput = predictedOutput; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}