package com.edu2job.security;

import com.edu2job.models.User;
import com.edu2job.repositories.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the {@link User} behind an {@code Authorization: Bearer <JWT>}
 * header. This centralizes the token handling that the old controllers each
 * duplicated; token creation/validation itself is unchanged (see
 * {@link JwtUtil}), so existing accounts and tokens keep working.
 *
 * <p>Every profile and prediction operation goes through this resolver, so
 * data access is always scoped to the authenticated user — request bodies
 * never carry a user id.</p>
 */
@Component
public class AuthUserResolver {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthUserResolver(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * @return the authenticated user, or empty when the header is missing,
     *         malformed, expired, or refers to an unknown user.
     */
    public Optional<User> resolve(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            String username = jwtUtil.extractUsername(authHeader.substring(7));
            if (username == null) {
                return Optional.empty();
            }
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            // Expired / forged / malformed token — treat as unauthenticated.
            return Optional.empty();
        }
    }
}
