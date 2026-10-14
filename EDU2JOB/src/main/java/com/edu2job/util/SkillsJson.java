package com.edu2job.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Codec for the multi-valued {@code skills} column.
 *
 * <p>New rows store skills as a JSON array string (e.g.
 * {@code ["Python","SQL"]}) in the existing PostgreSQL {@code TEXT} column,
 * so multi-word skills survive round-trips losslessly. Legacy rows written by
 * the old model stored a comma-joined string (e.g. {@code "Python,SQL"});
 * {@link #decode(String)} understands both formats, so existing data keeps
 * working without a data rewrite.</p>
 */
public final class SkillsJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SkillsJson() {
    }

    /** Serialize skills to the stored JSON-array representation. */
    public static String encode(List<String> skills) {
        if (skills == null) {
            return null;
        }
        List<String> cleaned = new ArrayList<>();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                cleaned.add(skill.trim());
            }
        }
        try {
            return MAPPER.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize skills", e);
        }
    }

    /**
     * Parse a stored skills value back into a list. Accepts the new JSON
     * array format and the legacy comma-joined format; null/blank yields an
     * empty list.
     */
    public static List<String> decode(String stored) {
        if (stored == null || stored.isBlank()) {
            return new ArrayList<>();
        }
        String trimmed = stored.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
                List<String> cleaned = new ArrayList<>();
                for (String skill : parsed) {
                    if (skill != null && !skill.isBlank()) {
                        cleaned.add(skill.trim());
                    }
                }
                return cleaned;
            } catch (Exception ignored) {
                // Fall through to legacy parsing below.
            }
        }
        List<String> skills = new ArrayList<>();
        for (String part : trimmed.split(",")) {
            if (!part.isBlank()) {
                skills.add(part.trim());
            }
        }
        return skills;
    }
}
