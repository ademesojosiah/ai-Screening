package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.service.InconsistencyScreener;
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
class InconsistencyConsumerTest {

    private static final String RESULT_TOPIC = "hireflow.screening.inconsistency.v1";

    @Mock private InconsistencyScreener inconsistencyScreener;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private InconsistencyConsumer consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "inconsistencyReviewCompletedTopic", RESULT_TOPIC);
    }

    @Test
    @DisplayName("Should run inconsistency review and publish the result to the correct topic")
    void consumeShouldRunInconsistencyReviewAndPublishResult() {
        ApplicationSubmittedEvent event = submittedEvent();
        InconsistencyReviewCompletedEvent result = new InconsistencyReviewCompletedEvent(
                "application-1", 15, "LOW", "Claims align", "No flags", "Proceed");
        when(inconsistencyScreener.detect(event)).thenReturn(result);

        consumer.consume(event);

        verify(inconsistencyScreener).detect(event);
        verify(kafkaTemplate).send(RESULT_TOPIC, "application-1", result);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should propagate screener exceptions so the container error handler can retry")
    void consumeShouldPropagateScreenerException() {
        ApplicationSubmittedEvent event = submittedEvent();
        when(inconsistencyScreener.detect(event)).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection refused");

        verifyNoInteractions(kafkaTemplate);
    }

    private ApplicationSubmittedEvent submittedEvent() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Python", "TensorFlow"));
        event.setApplicantSkills(List.of("Python"));
        event.setResumeSummary("ML engineer");
        return event;
    }
}
