package com.edu2job.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillsJsonTest {

    @Test
    void encodeDecodeRoundTripsMultiWordSkills() {
        List<String> skills = List.of("Python", "Data Analysis", "Project Management");
        assertEquals(skills, SkillsJson.decode(SkillsJson.encode(skills)));
    }

    @Test
    void decodeUnderstandsLegacyCommaJoinedValues() {
        assertEquals(List.of("Python", "SQL", "Java"), SkillsJson.decode("Python,SQL,Java"));
    }

    @Test
    void decodeHandlesNullAndBlank() {
        assertEquals(List.of(), SkillsJson.decode(null));
        assertEquals(List.of(), SkillsJson.decode("  "));
    }

    @Test
    void encodeNullStaysNull() {
        assertNull(SkillsJson.encode(null));
    }
}
