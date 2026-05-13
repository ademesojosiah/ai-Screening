package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.MatchSummariser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchSummaryConsumerTest {

    private static final String RESULT_TOPIC = "hireflow.screening.completed.v1";

    @Mock private MatchSummariser matchSummariser;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private MatchSummaryConsumer consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "screeningCompletedTopic", RESULT_TOPIC);
    }

    @Test
    @DisplayName("Should run match summary and publish the result to the correct topic")
    void consumeShouldRunMatchSummaryAndPublishResult() {
        ApplicationSubmittedEvent event = submittedEvent();
        ScreeningCompletedEvent result = new ScreeningCompletedEvent(
                "application-1", 72, List.of("Java", "Spring Boot"), List.of("Kafka"), "Strong match overall");
        when(matchSummariser.summarise(event)).thenReturn(result);

        consumer.consume(event);

        verify(matchSummariser).summarise(event);
        verify(kafkaTemplate).send(RESULT_TOPIC, "application-1", result);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should propagate summariser exceptions so the Kafka container can handle them")
    void consumeShouldPropagateSummariserException() {
        ApplicationSubmittedEvent event = submittedEvent();
        when(matchSummariser.summarise(event)).thenThrow(new RuntimeException("Rate limit exceeded"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rate limit exceeded");

        verify(kafkaTemplate, never()).send(RESULT_TOPIC, "application-1", null);
    }

    private ApplicationSubmittedEvent submittedEvent() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka", "Spring Boot"));
        event.setApplicantSkills(List.of("Java", "Spring Boot"));
        event.setResumeSummary("Backend engineer");
        return event;
    }
}
