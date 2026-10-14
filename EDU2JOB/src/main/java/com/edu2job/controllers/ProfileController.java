package com.edu2job.controllers;

import com.edu2job.dto.ProfileResponse;
import com.edu2job.dto.ProfileUpdateRequest;
import com.edu2job.models.User;
import com.edu2job.security.AuthUserResolver;
import com.edu2job.service.ProfileService;
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
 * Profile endpoints (routes unchanged from the previous version):
 * {@code POST /api/profile/update} and {@code GET /api/profile/latest}.
 * Both are scoped to the JWT-authenticated user.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthUserResolver authUserResolver;

    public ProfileController(ProfileService profileService, AuthUserResolver authUserResolver) {
        this.profileService = profileService;
        this.authUserResolver = authUserResolver;
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Optional<User> user = authUserResolver.resolve(authHeader);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        ProfileResponse response = profileService.updateProfile(user.get(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> user = authUserResolver.resolve(authHeader);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return profileService.getLatestProfile(user.get())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No profile found.")));
    }
}
