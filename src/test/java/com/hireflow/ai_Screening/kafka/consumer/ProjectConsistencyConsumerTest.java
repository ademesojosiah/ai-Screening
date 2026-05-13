package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import com.hireflow.ai_Screening.service.ProjectConsistencyScreener;
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
class ProjectConsistencyConsumerTest {

    private static final String RESULT_TOPIC = "hireflow.screening.project.v1";

    @Mock private ProjectConsistencyScreener projectConsistencyScreener;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private ProjectConsistencyConsumer consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "projectConsistencyCompletedTopic", RESULT_TOPIC);
    }

    @Test
    @DisplayName("Should run project consistency check and publish the result to the correct topic")
    void consumeShouldRunConsistencyCheckAndPublishResult() {
        ApplicationSubmittedEvent event = submittedEvent();
        ProjectConsistencyCompletedEvent result = new ProjectConsistencyCompletedEvent("application-1", 80, "Projects align with skills", "No issues");
        when(projectConsistencyScreener.score(event)).thenReturn(result);

        consumer.consume(event);

        verify(projectConsistencyScreener).score(event);
        verify(kafkaTemplate).send(RESULT_TOPIC, "application-1", result);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should propagate screener exceptions so the container error handler can retry")
    void consumeShouldPropagateScreenerException() {
        ApplicationSubmittedEvent event = submittedEvent();
        when(projectConsistencyScreener.score(event)).thenThrow(new RuntimeException("Service unavailable"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service unavailable");

        verify(kafkaTemplate, never()).send(RESULT_TOPIC, "application-1", null);
    }

    private ApplicationSubmittedEvent submittedEvent() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Spring Boot"));
        event.setApplicantSkills(List.of("Java", "Spring Boot"));
        event.setResumeSummary("Senior backend developer");
        return event;
    }
}
