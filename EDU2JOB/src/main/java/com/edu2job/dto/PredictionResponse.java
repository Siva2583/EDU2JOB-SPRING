package com.edu2job.dto;

import java.util.List;

/**
 * Stable prediction contract returned by Spring Boot to React:
 *
 * <pre>
 * {
 *   "predictionId": 101,
 *   "modelVersion": "edu2job-v2",
 *   "predictions": [ { "rank": 1, "role": "Data Analyst", "score": null } ]
 * }
 * </pre>
 */
public class PredictionResponse {

    private Long predictionId;
    private String modelVersion;
    private List<RankedRole> predictions;

    public PredictionResponse() {
    }

    public PredictionResponse(Long predictionId, String modelVersion, List<RankedRole> predictions) {
        this.predictionId = predictionId;
        this.modelVersion = modelVersion;
        this.predictions = predictions;
    }

    public Long getPredictionId() { return predictionId; }
    public void setPredictionId(Long predictionId) { this.predictionId = predictionId; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public List<RankedRole> getPredictions() { return predictions; }
    public void setPredictions(List<RankedRole> predictions) { this.predictions = predictions; }
}
