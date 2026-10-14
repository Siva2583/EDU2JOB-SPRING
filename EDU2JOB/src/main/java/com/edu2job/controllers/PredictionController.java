package com.edu2job.controllers;

import com.edu2job.dto.PredictionRequest;
import com.edu2job.models.User;
import com.edu2job.security.AuthUserResolver;
import com.edu2job.service.MlServiceException;
import com.edu2job.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Prediction endpoints (routes unchanged):
 * {@code POST /api/predict/} runs a nine-field prediction through the Flask
 * ML service and archives it; {@code GET /api/predict/history} returns the
 * authenticated user's history (old-model rows included, marked by a null
 * model version).
 */
@RestController
@RequestMapping("/api/predict")
public class PredictionController {

    private final PredictionService predictionService;
    private final AuthUserResolver authUserResolver;

    public PredictionController(PredictionService predictionService, AuthUserResolver authUserResolver) {
        this.predictionService = predictionService;
        this.authUserResolver = authUserResolver;
    }

    @PostMapping("/")
    public ResponseEntity<?> makePrediction(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody PredictionRequest request) {
        Optional<User> user = authUserResolver.resolve(authHeader);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        try {
            return ResponseEntity.ok(predictionService.predict(user.get(), request));
        } catch (MlServiceException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> user = authUserResolver.resolve(authHeader);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return ResponseEntity.ok(predictionService.getHistory(user.get()));
    }
}
