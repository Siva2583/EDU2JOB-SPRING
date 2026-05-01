package com.edu2job.controllers;
import com.edu2job.models.User;
import com.edu2job.models.UserPredictionHistory;
import com.edu2job.repositories.UserPredictionHistoryRepository;
import com.edu2job.repositories.UserRepository;
import com.edu2job.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserPredictionHistoryRepository profileRepo;

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

    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> data) {
        User user = getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String skills = data.getOrDefault("skills", "").toString();
        String major = data.getOrDefault("major", "").toString();
        String degree = data.getOrDefault("degree", "").toString();
        
        Double cgpa = null;
        if (data.get("cgpa") != null && !data.get("cgpa").toString().isEmpty()) {
            cgpa = Double.valueOf(data.get("cgpa").toString());
        }

        Integer yop = null;
        if (data.get("yop") != null && !data.get("yop").toString().isEmpty()) {
            yop = Integer.valueOf(data.get("yop").toString());
        }

        Optional<UserPredictionHistory> lastProfileOpt = profileRepo.findFirstByUserOrderByCreatedAtDesc(user);
        UserPredictionHistory profile;

        if (lastProfileOpt.isPresent()) {
            profile = lastProfileOpt.get();
            if (!major.isEmpty()) profile.setMajor(major);
            if (cgpa != null) profile.setCgpa(cgpa);
            if (!degree.isEmpty()) profile.setDegree(degree);
            if (!skills.isEmpty()) profile.setSkills(skills);
            if (yop != null) profile.setYearOfGraduation(yop);
        } else {
            profile = new UserPredictionHistory();
            profile.setUser(user);
            profile.setMajor(major);
            profile.setCgpa(cgpa);
            profile.setDegree(degree);
            profile.setSkills(skills);
            profile.setYearOfGraduation(yop);
        }

        profileRepo.save(profile);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile updated successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestProfile(@RequestHeader("Authorization") String authHeader) {
        User user = getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Optional<UserPredictionHistory> lastProfileOpt = profileRepo.findFirstByUserOrderByCreatedAtDesc(user);

        if (lastProfileOpt.isPresent()) {
            UserPredictionHistory p = lastProfileOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("major", p.getMajor());
            response.put("cgpa", p.getCgpa());
            response.put("degree", p.getDegree());
            response.put("skills", p.getSkills() != null && !p.getSkills().isEmpty() ? Arrays.asList(p.getSkills().split(",")) : new java.util.ArrayList<>());
            response.put("year_of_graduation", p.getYearOfGraduation());
            return ResponseEntity.ok(response);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "No profile found.");
        return ResponseEntity.ok(response);
    }
}