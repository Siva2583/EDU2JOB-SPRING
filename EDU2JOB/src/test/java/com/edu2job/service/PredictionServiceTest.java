package com.edu2job.service;

import com.edu2job.dto.PredictionRequest;
import com.edu2job.dto.PredictionResponse;
import com.edu2job.models.PredHistory;
import com.edu2job.models.User;
import com.edu2job.repositories.PredHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prediction-flow tests with the Flask service mocked at the RestTemplate
 * boundary. NOTE: authored during the migration; run with {@code mvn test}
 * (the migration sandbox had no JDK, so these were not executed there).
 */
@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private PredHistoryRepository repository;

    @Mock
    private RestTemplate restTemplate;

    private PredictionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PredictionService(repository, restTemplate, new ObjectMapper(),
                "http://localhost:5000");
        user = new User("alice", "hashed");
        user.setId(7L);
        when(repository.save(any(PredHistory.class))).thenAnswer(invocation -> {
            PredHistory history = invocation.getArgument(0);
            history.setId(101L);
            return history;
        });
    }

    private PredictionRequest validRequest() {
        PredictionRequest request = new PredictionRequest();
        request.setDegree("B.Tech");
        request.setMajor("Computer Science");
        request.setCgpa(new BigDecimal("8.5"));
        request.setSkills(List.of("Python", "SQL"));
        request.setInternshipExperience("Yes");
        request.setProjectsCount(3);
        request.setCommunicationSkill("Advanced");
        request.setExperienceMonths(6);
        return request;
    }

    @Test
    void sendsNineFieldsToFlaskAndPersistsResult() {
        String flaskJson = "{\"modelVersion\":\"edu2job-v2\",\"predictions\":["
                + "{\"rank\":1,\"role\":\"Data Analyst\",\"score\":null},"
                + "{\"rank\":2,\"role\":\"Software Engineer\",\"score\":null}]}";
        when(restTemplate.postForEntity(eq("http://localhost:5000/predict"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(flaskJson, HttpStatus.OK));

        PredictionResponse response = service.predict(user, validRequest());

        assertEquals(101L, response.getPredictionId());
        assertEquals("edu2job-v2", response.getModelVersion());
        assertEquals(2, response.getPredictions().size());
        assertNull(response.getPredictions().get(0).getScore());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
        Map<String, Object> sent = captor.getValue().getBody();
        assertEquals(9, sent.size());
        assertEquals(List.of("Python", "SQL"), sent.get("skills"));

        ArgumentCaptor<PredHistory> historyCaptor = ArgumentCaptor.forClass(PredHistory.class);
        verify(repository).save(historyCaptor.capture());
        assertEquals("edu2job-v2", historyCaptor.getValue().getModelVersion());
        assertEquals(user, historyCaptor.getValue().getUser());
    }

    @Test
    void unreachableFlaskYields503WithoutPersisting() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        MlServiceException ex = assertThrows(MlServiceException.class,
                () -> service.predict(user, validRequest()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }
}
