package com.edu2job.service;

import com.edu2job.dto.HistoryEntryResponse;
import com.edu2job.dto.PredictionRequest;
import com.edu2job.dto.PredictionResponse;
import com.edu2job.dto.RankedRole;
import com.edu2job.models.PredHistory;
import com.edu2job.models.User;
import com.edu2job.repositories.PredHistoryRepository;
import com.edu2job.util.SkillsJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The EDU2JOB 2.0 prediction flow:
 *
 * <ol>
 *   <li>Take the already-authenticated user and the validated nine-field
 *       {@link PredictionRequest}.</li>
 *   <li>Forward exactly those nine fields to the Flask ML service
 *       ({@code edu2job.flask.base-url}, never hardcoded).</li>
 *   <li>Translate Flask failures into safe, typed errors — the new model is
 *       never silently replaced by the old one or by fabricated output.</li>
 *   <li>Persist inputs, raw model output and model version, then return the
 *       stable {@link PredictionResponse} contract.</li>
 * </ol>
 */
@Service
public class PredictionService {

    /** Fallback version label if the ML service does not report one. */
    static final String DEFAULT_MODEL_VERSION = "edu2job-v2";

    private final PredHistoryRepository predHistoryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String flaskBaseUrl;

    public PredictionService(PredHistoryRepository predHistoryRepository,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             @Value("${edu2job.flask.base-url:http://localhost:5000}") String flaskBaseUrl) {
        this.predHistoryRepository = predHistoryRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.flaskBaseUrl = flaskBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public PredictionResponse predict(User user, PredictionRequest request) {
        Map<String, Object> payload = request.toFlaskPayload();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        String flaskBody;
        try {
            ResponseEntity<String> flaskResponse =
                    restTemplate.postForEntity(flaskBaseUrl + "/predict", entity, String.class);
            flaskBody = flaskResponse.getBody();
        } catch (ResourceAccessException e) {
            // Connection refused / timeout — ML service unreachable.
            throw new MlServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The career recommendation service is currently unavailable. Please try again shortly.");
        } catch (HttpStatusCodeException e) {
            throw mapFlaskError(e);
        } catch (Exception e) {
            throw new MlServiceException(HttpStatus.BAD_GATEWAY,
                    "The career recommendation service returned an unexpected response.");
        }

        ParsedFlaskResponse parsed = parseFlaskResponse(flaskBody);

        PredHistory history = new PredHistory();
        history.setUser(user);
        history.setDegree(request.getDegree());
        history.setMajor(request.getMajor());
        history.setCgpa(request.getCgpa());
        history.setSkills(SkillsJson.encode(request.getSkills()));
        history.setCertificationName(emptyToNull(request.getCertificationName()));
        history.setInternshipExperience(request.getInternshipExperience());
        history.setProjectsCount(request.getProjectsCount());
        history.setCommunicationSkill(request.getCommunicationSkill());
        history.setExperienceMonths(request.getExperienceMonths());
        history.setModelVersion(parsed.modelVersion());
        history.setPredictedOutput(flaskBody);
        PredHistory saved = predHistoryRepository.save(history);

        return new PredictionResponse(saved.getId(), parsed.modelVersion(), parsed.predictions());
    }

    @Transactional(readOnly = true)
    public List<HistoryEntryResponse> getHistory(User user) {
        return predHistoryRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(HistoryEntryResponse::from)
                .toList();
    }

    /** Map a Flask error response to a safe API error (no internals leaked). */
    private MlServiceException mapFlaskError(HttpStatusCodeException e) {
        String flaskMessage = extractFlaskMessage(e.getResponseBodyAsString());
        int status = e.getStatusCode().value();
        if (status == 400) {
            return new MlServiceException(HttpStatus.BAD_REQUEST,
                    flaskMessage != null ? flaskMessage
                            : "The recommendation service rejected the profile data.");
        }
        if (status == 503) {
            return new MlServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The career recommendation model is not ready yet. New model artifacts are still being installed.");
        }
        return new MlServiceException(HttpStatus.BAD_GATEWAY,
                "The career recommendation service returned an unexpected response.");
    }

    private String extractFlaskMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.get("error");
            if (error != null && error.hasNonNull("message")) {
                return error.get("message").asText();
            }
            if (error != null && error.isTextual()) {
                return error.asText();
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Validate and decode the Flask response. Scores are carried through
     * only when the model actually returned a number; string scores from the
     * legacy contract are parsed, anything else becomes {@code null} — never
     * an invented value.
     */
    private ParsedFlaskResponse parseFlaskResponse(String flaskBody) {
        if (flaskBody == null || flaskBody.isBlank()) {
            throw new MlServiceException(HttpStatus.BAD_GATEWAY,
                    "The career recommendation service returned an empty response.");
        }
        try {
            JsonNode root = objectMapper.readTree(flaskBody);
            JsonNode predictionsNode = root.get("predictions");
            if (predictionsNode == null || !predictionsNode.isArray()) {
                throw new MlServiceException(HttpStatus.BAD_GATEWAY,
                        "The career recommendation service returned an invalid response.");
            }
            List<RankedRole> predictions = new ArrayList<>();
            int rank = 1;
            for (JsonNode node : predictionsNode) {
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
                predictions.add(new RankedRole(nodeRank, role, score));
                if (predictions.size() == 5) {
                    break;
                }
                rank++;
            }
            if (predictions.isEmpty()) {
                throw new MlServiceException(HttpStatus.BAD_GATEWAY,
                        "The career recommendation service returned no recommendations.");
            }
            String modelVersion = root.path("modelVersion").asText(null);
            if (modelVersion == null || modelVersion.isBlank()) {
                JsonNode versionNode = root.get("model_version");
                modelVersion = versionNode != null ? versionNode.asText(null) : null;
            }
            if (modelVersion == null || modelVersion.isBlank()) {
                modelVersion = DEFAULT_MODEL_VERSION;
            }
            return new ParsedFlaskResponse(List.copyOf(predictions), modelVersion);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException(HttpStatus.BAD_GATEWAY,
                    "The career recommendation service returned an invalid response.");
        }
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record ParsedFlaskResponse(List<RankedRole> predictions, String modelVersion) {
    }
}
