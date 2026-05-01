package com.edu2job.controllers;

import com.edu2job.models.User;
import com.edu2job.repositories.UserRepository;
import com.edu2job.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            if (userRepository.existsByUsername(username)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Username is already taken.");
                return ResponseEntity.badRequest().body(response);
            }

            User user = new User(username, passwordEncoder.encode(password));
            userRepository.save(user);

            String token = jwtUtil.generateToken(username);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Welcome " + username + "!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Server Error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
                String token = jwtUtil.generateToken(username);
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("message", "Welcome back, " + username + "!");
                return ResponseEntity.ok(response);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invalid username or password.");
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Server Error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}