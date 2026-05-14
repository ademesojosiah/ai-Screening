package com.hireflow.ai_Screening.restclient.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hireflow.ai_Screening.config.GeminiConfig.GeminiSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SpringGeminiChatClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should parse JSON object returned inside chat completion message content")
    void completeJson_parsesMessageContent() {
        ClientFixture fixture = clientFixture();
        String content = """
                {
                  "score": 91,
                  "explanation": "Good skill overlap.",
                  "review": "Strong backend match."
                }
                """;
        ObjectNode responseEnvelope = objectMapper.createObjectNode();
        responseEnvelope.putArray("choices")
                .addObject()
                .putObject("message")
                .put("content", content);
        String response = responseEnvelope.toString();

        fixture.server().expect(requestTo("https://gemini.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(jsonPath("$.model").value("gemini-test"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        JsonNode result = fixture.client().completeJson("system", "user");

        assertThat(result.path("score").asInt()).isEqualTo(91);
        assertThat(result.path("explanation").asText()).isEqualTo("Good skill overlap.");
        fixture.server().verify();
    }

    @Test
    @DisplayName("Should wrap malformed provider envelope as chat exception")
    void completeJson_wrapsMalformedEnvelope() {
        ClientFixture fixture = clientFixture();

        fixture.server().expect(requestTo("https://gemini.test/chat/completions"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client().completeJson("system", "user"))
                .isInstanceOf(GeminiChatException.class)
                .hasMessage("Gemini chat completion failed");
        fixture.server().verify();
    }

    private ClientFixture clientFixture() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://gemini.test")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-api-key")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SpringGeminiChatClient client = new SpringGeminiChatClient(
                restClientBuilder.build(),
                new GeminiSettings("gemini-test", 0.2),
                objectMapper
        );
        return new ClientFixture(client, server);
    }

    private record ClientFixture(SpringGeminiChatClient client, MockRestServiceServer server) {
    }
}
