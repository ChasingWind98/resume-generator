package de.thi.resume.dto;

import java.util.List;

public record ResumeData(
        PersonalInfo personalInfo,
        List<String> skills,
        List<EducationEntry> educationEntries,
        List<ExperienceEntry> experienceEntries,
        List<ProjectExperienceEntry> projectExperienceEntries,
        String photoPath
) {
}
