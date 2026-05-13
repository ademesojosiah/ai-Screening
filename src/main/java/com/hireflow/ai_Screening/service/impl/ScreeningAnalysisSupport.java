package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedAnswer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ScreeningAnalysisSupport {

    private ScreeningAnalysisSupport() {}

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    static Set<String> normalize(List<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : safeList(values)) {
            normalized.add(normalize(value));
        }
        return normalized;
    }

    static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static boolean containsNormalized(String text, String value) {
        return normalize(text).contains(normalize(value));
    }

    static String combineEvidence(String resumeSummary, List<ApplicationSubmittedAnswer> answers) {
        StringBuilder builder = new StringBuilder();
        if (resumeSummary != null) {
            builder.append(resumeSummary);
        }
        for (ApplicationSubmittedAnswer answer : safeList(answers)) {
            if (answer != null && answer.getApplicantAnswer() != null) {
                builder.append(' ').append(answer.getApplicantAnswer());
            }
        }
        return builder.toString();
    }

    static Set<String> significantTokens(String value) {
        Set<String> tokens = new HashSet<>();
        if (value == null) {
            return tokens;
        }
        for (String token : value.split("[^a-z0-9+#.]+")) {
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * Token-safe skill lookup: splits both evidence and skill into word tokens so that
     * "Java" does not falsely match inside "JavaScript", and multi-word skills like
     * "Spring Boot" require both tokens to be present.
     * Falls back to substring matching for very short skills whose tokens are empty (e.g. "Go").
     */
    static boolean evidenceSupportsSkill(String evidence, String skill) {
        if (isBlank(evidence) || isBlank(skill)) return false;
        Set<String> evidenceTokens = significantTokens(normalize(evidence));
        Set<String> skillTokens = significantTokens(normalize(skill));
        if (skillTokens.isEmpty()) {
            return containsNormalized(evidence, skill);
        }
        return evidenceTokens.containsAll(skillTokens);
    }
}
