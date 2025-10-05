package de.thi.resume.dto;

import java.util.List;

/**
 * Berufserfahrung Eintrag
 */
public record ExperienceEntry(
        String title,
        String firma,
        String zeitraum,
        List<String> beschreibung
) {
}
