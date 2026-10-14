package com.edu2job.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean Validation + Flask-payload contract tests for the nine-field schema.
 *
 * NOTE (honest test status): these tests were authored during the migration
 * but could not be executed in the migration sandbox (no JDK/Maven was
 * available there). Run them with:  cd EDU2JOB && mvn test
 */
class PredictionRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private PredictionRequest validRequest() {
        PredictionRequest request = new PredictionRequest();
        request.setDegree("B.Tech");
        request.setMajor("Computer Science");
        request.setCgpa(new BigDecimal("8.5"));
        request.setSkills(List.of("Python", "SQL"));
        request.setCertificationName(null);
        request.setInternshipExperience("Yes");
        request.setProjectsCount(3);
        request.setCommunicationSkill("Advanced");
        request.setExperienceMonths(6);
        return request;
    }

    @Test
    void validNineFieldRequestPasses() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void cgpaAboveTenIsRejected() {
        PredictionRequest request = validRequest();
        request.setCgpa(new BigDecimal("10.5"));
        Set<ConstraintViolation<PredictionRequest>> violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cgpa")));
    }

    @Test
    void negativeProjectsAndExperienceAreRejected() {
        PredictionRequest request = validRequest();
        request.setProjectsCount(-1);
        request.setExperienceMonths(-2);
        Set<ConstraintViolation<PredictionRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        PredictionRequest request = new PredictionRequest();
        assertTrue(validator.validate(request).size() >= 5);
    }

    @Test
    void flaskPayloadContainsExactlyTheNineContractFields() {
        Map<String, Object> payload = validRequest().toFlaskPayload();
        assertEquals(List.of("degree", "major", "cgpa", "skills", "certification_name",
                "internship_experience", "projects_count", "communication_skill",
                "experience_months"), List.copyOf(payload.keySet()));
    }
}
