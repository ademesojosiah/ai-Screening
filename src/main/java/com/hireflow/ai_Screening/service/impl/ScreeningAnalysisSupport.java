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
}
