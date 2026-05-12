package com.hireflow.ai_Screening.kafka;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.AiScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationSubmittedConsumer {

    private final AiScreeningService aiScreeningService;
    private final KafkaTemplate<String, ScreeningCompletedEvent> kafkaTemplate;

    @Value("${hireflow.kafka.topics.screening-completed}")
    private String screeningCompletedTopic;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.application-submitted}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ApplicationSubmittedEvent event) {
        log.info("Received application submitted event {}", event.getApplicationId());
        ScreeningCompletedEvent result = aiScreeningService.screen(event);
        kafkaTemplate.send(screeningCompletedTopic, result.getApplicationId(), result);
        log.info("Published screening completed event {}", result.getApplicationId());
    }
}
