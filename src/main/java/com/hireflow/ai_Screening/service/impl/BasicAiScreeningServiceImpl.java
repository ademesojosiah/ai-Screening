package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.AiScreeningService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BasicAiScreeningServiceImpl implements AiScreeningService {

    @Override
    public ScreeningCompletedEvent screen(ApplicationSubmittedEvent event) {
        List<String> jobSkills = safeList(event.getJobSkills());
        Set<String> applicantSkills = normalize(safeList(event.getApplicantSkills()));

        List<String> matchedSkills = jobSkills.stream()
                .filter(skill -> applicantSkills.contains(normalize(skill)))
                .toList();

        List<String> unmatchedSkills = jobSkills.stream()
                .filter(skill -> !applicantSkills.contains(normalize(skill)))
                .toList();

        int score = calculateScore(jobSkills, matchedSkills);
        String summary = "Matched " + matchedSkills.size() + " of " + jobSkills.size() + " required job skills.";

        return new ScreeningCompletedEvent(
                event.getApplicationId(),
                score,
                matchedSkills,
                unmatchedSkills,
                summary
        );
    }

    private int calculateScore(List<String> jobSkills, List<String> matchedSkills) {
        if (jobSkills.isEmpty()) {
            return 50;
        }
        return (int) Math.round((matchedSkills.size() * 100.0) / jobSkills.size());
    }

    private Set<String> normalize(List<String> skills) {
        Set<String> normalized = new HashSet<>();
        for (String skill : skills) {
            normalized.add(normalize(skill));
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
