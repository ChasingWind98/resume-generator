package de.thi.resume.dto;

import java.util.List;

public record ProjectExperienceEntry(
        String projektTitel,
        String technologien,
        String link,
        List<String> beschreibung
) {
}