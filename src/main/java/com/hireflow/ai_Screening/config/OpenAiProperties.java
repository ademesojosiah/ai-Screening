package com.hireflow.ai_Screening.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "hireflow.ai.openai")
public class OpenAiProperties {

    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private double temperature = 0.2;
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 30_000;
}
