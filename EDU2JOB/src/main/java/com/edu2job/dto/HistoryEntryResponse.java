package com.edu2job.dto;

import com.edu2job.models.PredHistory;
import com.edu2job.util.SkillsJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One prediction-history entry, exposing the nine stored input fields, the
 * decoded ranked roles and the model version that produced them.
 *
 * <p>Legacy rows written by the old model have a {@code null} model version
 * and null new-schema fields. They are returned as-is — never reinterpreted
 * as new-model predictions — and the UI marks them as legacy.</p>
 */
public class HistoryEntryResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
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

    /** Version reported by the ML service; {@code null} for legacy (v1) rows. */
    private String modelVersion;

    private List<RankedRole> predictions;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static HistoryEntryResponse from(PredHistory history) {
        HistoryEntryResponse response = new HistoryEntryResponse();
        response.id = history.getId();
        response.degree = history.getDegree();
        response.major = history.getMajor();
        response.cgpa = history.getCgpa();
        response.skills = SkillsJson.decode(history.getSkills());
        response.certificationName = history.getCertificationName();
        response.internshipExperience = history.getInternshipExperience();
        response.projectsCount = history.getProjectsCount();
        response.communicationSkill = history.getCommunicationSkill();
        response.experienceMonths = history.getExperienceMonths();
        response.modelVersion = history.getModelVersion();
        response.predictions = parsePredictions(history.getPredictedOutput());
        response.createdAt = history.getCreatedAt();
        return response;
    }

    /**
     * Decode the archived raw model output. Both the new contract
     * ({@code predictions: [{rank, role, score}]}) and the legacy contract
     * ({@code predictions: [{role, score: "85.0", scope}]}) are understood;
     * anything unparsable yields an empty list rather than an error.
     */
    static List<RankedRole> parsePredictions(String predictedOutput) {
        List<RankedRole> roles = new ArrayList<>();
        if (predictedOutput == null || predictedOutput.isBlank()) {
            return roles;
        }
        try {
            JsonNode root = MAPPER.readTree(predictedOutput);
            JsonNode predictions = root.get("predictions");
            if (predictions == null || !predictions.isArray()) {
                return roles;
            }
            int rank = 1;
            for (JsonNode node : predictions) {
                String role = node.path("role").asText(null);
                if (role == null || role.isBlank()) {
                    continue;
                }
                Double score = null;
                JsonNode scoreNode = node.get("score");
                if (scoreNode != null && scoreNode.isNumber()) {
                    score = scoreNode.asDouble();
                } else if (scoreNode != null && scoreNode.isTextual()) {
                    try {
                        score = Double.valueOf(scoreNode.asText());
                    } catch (NumberFormatException ignored) {
                        score = null;
                    }
                }
                int nodeRank = node.path("rank").asInt(rank);
                roles.add(new RankedRole(nodeRank, role, score));
                rank++;
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return roles;
    }

    public Long getId() { return id; }
    public String getDegree() { return degree; }
    public String getMajor() { return major; }
    public BigDecimal getCgpa() { return cgpa; }
    public List<String> getSkills() { return skills; }
    public String getCertificationName() { return certificationName; }
    public String getInternshipExperience() { return internshipExperience; }
    public Integer getProjectsCount() { return projectsCount; }
    public String getCommunicationSkill() { return communicationSkill; }
    public Integer getExperienceMonths() { return experienceMonths; }
    public String getModelVersion() { return modelVersion; }
    public List<RankedRole> getPredictions() { return predictions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
