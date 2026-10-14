package com.edu2job.service;

import com.edu2job.dto.ProfileResponse;
import com.edu2job.dto.ProfileUpdateRequest;
import com.edu2job.models.User;
import com.edu2job.models.UserPredictionHistory;
import com.edu2job.repositories.UserPredictionHistoryRepository;
import com.edu2job.util.SkillsJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * NOTE: authored during the migration; run with {@code mvn test} (the
 * migration sandbox had no JDK, so these were not executed there).
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserPredictionHistoryRepository repository;

    private ProfileService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ProfileService(repository);
        user = new User("alice", "hashed");
        user.setId(1L);
        when(repository.save(any(UserPredictionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsProfileWithAllNineFields() {
        when(repository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDegree("BBA");
        request.setMajor("Finance");
        request.setCgpa(new BigDecimal("8.20"));
        request.setSkills(List.of("Excel", "Financial Analysis"));
        request.setCertificationName(null);
        request.setInternshipExperience("Yes");
        request.setProjectsCount(2);
        request.setCommunicationSkill("Intermediate");
        request.setExperienceMonths(4);

        ProfileResponse response = service.updateProfile(user, request);

        assertEquals("BBA", response.getDegree());
        assertEquals(List.of("Excel", "Financial Analysis"), response.getSkills());
        assertNull(response.getCertificationName());
        assertEquals(9 - 1, response.completedFieldCount()); // all but certification
    }

    @Test
    void partialUpdatePreservesFieldsNotSupplied() {
        UserPredictionHistory existing = new UserPredictionHistory();
        existing.setUser(user);
        existing.setDegree("B.Tech");
        existing.setMajor("Computer Science");
        existing.setCgpa(new BigDecimal("7.50"));
        existing.setSkills(SkillsJson.encode(List.of("Java")));
        when(repository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(existing));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setCgpa(new BigDecimal("8.00")); // only CGPA changes

        ProfileResponse response = service.updateProfile(user, request);

        assertEquals("B.Tech", response.getDegree());
        assertEquals("Computer Science", response.getMajor());
        assertEquals(List.of("Java"), response.getSkills());
        assertEquals(new BigDecimal("8.00"), response.getCgpa());
    }

    @Test
    void explicitEmptySkillsListClearsSkills() {
        UserPredictionHistory existing = new UserPredictionHistory();
        existing.setUser(user);
        existing.setSkills(SkillsJson.encode(List.of("Java")));
        when(repository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(existing));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setSkills(List.of());

        assertEquals(List.of(), service.updateProfile(user, request).getSkills());
    }
}
