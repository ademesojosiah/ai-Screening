package com.hireflow.ai_Screening.kafka;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.AiScreeningService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationSubmittedConsumerTest {

    @Mock private AiScreeningService aiScreeningService;
    @Mock private KafkaTemplate<String, ScreeningCompletedEvent> kafkaTemplate;
    @InjectMocks private ApplicationSubmittedConsumer consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "screeningCompletedTopic", "hireflow.screening.completed.v1");
    }

    @Test
    @DisplayName("Should screen consumed application event and publish completed result")
    void consume_success() {
        ApplicationSubmittedEvent submitted = submittedEvent();
        ScreeningCompletedEvent completed = new ScreeningCompletedEvent(
                "application-1",
                67,
                List.of("Java", "Spring Boot"),
                List.of("Kafka"),
                "Matched 2 of 3 required job skills."
        );
        when(aiScreeningService.screen(submitted)).thenReturn(completed);

        consumer.consume(submitted);

        verify(aiScreeningService).screen(submitted);
        verify(kafkaTemplate).send("hireflow.screening.completed.v1", "application-1", completed);
    }

    private ApplicationSubmittedEvent submittedEvent() {
        return new ApplicationSubmittedEvent(
                "application-1",
                "job-1",
                "Backend Engineer",
                "Build APIs",
                "Java and Kafka",
                "Cloud experience",
                "applicant-1",
                "ada@example.com",
                "Backend engineer",
                "https://cdn.example.com/resume.pdf",
                List.of("Java", "Kafka", "Spring Boot"),
                List.of("Java", "Spring Boot"),
                40,
                75
        );
    }
}
