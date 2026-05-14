package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

final class GeminiResponseSupport {

    private GeminiResponseSupport() {}

    static Integer clampedScore(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.canConvertToInt()) {
            throw new IllegalArgumentException("Missing or non-integer '" + field + "' in AI provider response");
        }
        int value = node.asInt();
        if (value < 0) value = 0;
        if (value > 100) value = 100;
        return value;
    }

    static String requireText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing '" + field + "' in AI provider response");
        }
        String value = node.asText("").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty '" + field + "' in AI provider response");
        }
        return value;
    }

    static List<String> stringArray(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isArray()) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>(node.size());
        node.forEach(item -> {
            if (item != null && !item.isNull()) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) values.add(text);
            }
        });
        return values;
    }
}
