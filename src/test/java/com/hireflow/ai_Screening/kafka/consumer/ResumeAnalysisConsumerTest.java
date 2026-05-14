package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;
import com.hireflow.ai_Screening.service.ResumeAnalysisScreener;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisConsumerTest {

    private static final String RESULT_TOPIC = "hireflow.screening.resume.v1";

    @Mock private ResumeAnalysisScreener resumeAnalysisScreener;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private ResumeAnalysisConsumer consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "resumeAnalysisCompletedTopic", RESULT_TOPIC);
    }

    @Test
    @DisplayName("Should run resume analysis and publish the result to the correct topic")
    void consumeShouldRunAnalysisAndPublishResult() {
        ApplicationSubmittedEvent event = submittedEvent();
        ResumeAnalysisCompletedEvent result = new ResumeAnalysisCompletedEvent("application-1", 75, "Strong Java background", "No gaps");
        when(resumeAnalysisScreener.analyze(event)).thenReturn(result);

        consumer.consume(event);

        verify(resumeAnalysisScreener).analyze(event);
        verify(kafkaTemplate).send(RESULT_TOPIC, "application-1", result);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should propagate screener exceptions so the container error handler can retry")
    void consumeShouldPropagateScreenerException() {
        ApplicationSubmittedEvent event = submittedEvent();
        when(resumeAnalysisScreener.analyze(event)).thenThrow(new RuntimeException("Gemini timeout"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gemini timeout");

        verifyNoInteractions(kafkaTemplate);
    }

    private ApplicationSubmittedEvent submittedEvent() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka"));
        event.setApplicantSkills(List.of("Java"));
        event.setResumeSummary("Backend engineer");
        return event;
    }
}
