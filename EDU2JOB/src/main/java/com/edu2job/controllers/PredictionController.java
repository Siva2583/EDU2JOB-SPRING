package com.edu2job.controllers;
import com.edu2job.models.PredHistory;
import com.edu2job.models.User;
import com.edu2job.repositories.PredHistoryRepository;
import com.edu2job.repositories.UserRepository;
import com.edu2job.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/predict")
public class PredictionController {
    @Autowired
    private PredHistoryRepository predHistoryRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    private User getAuthenticatedUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        return userRepository.findByUsername(username).orElse(null);
    }
    @PostMapping("/")
    public ResponseEntity<?> makePrediction(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> data) {
        User user = getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        RestTemplate restTemplate = new RestTemplate();
        String flaskUrl = "http://localhost:5000/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> flaskResponse = restTemplate.postForEntity(flaskUrl, entity, String.class);
            PredHistory history = new PredHistory();
            history.setUser(user);
            history.setMajor(data.getOrDefault("major", "").toString());
            if (data.get("cgpa") != null && !data.get("cgpa").toString().isEmpty()) {
                history.setCgpa(Double.valueOf(data.get("cgpa").toString()));
            }
            history.setDegree(data.getOrDefault("degree", "").toString());
            history.setSkills(data.getOrDefault("skills", "").toString());
            
            if (data.get("yop") != null && !data.get("yop").toString().isEmpty()) {
                history.setYearOfGraduation(Integer.valueOf(data.get("yop").toString()));
            }

            history.setPredictedOutput(flaskResponse.getBody());
            predHistoryRepo.save(history);

            return ResponseEntity.ok(flaskResponse.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestHeader("Authorization") String authHeader) {
        User user = getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        List<PredHistory> history = predHistoryRepo.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(history);
    }
}