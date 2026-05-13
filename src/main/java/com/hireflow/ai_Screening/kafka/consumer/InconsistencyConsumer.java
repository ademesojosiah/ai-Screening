package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.service.InconsistencyScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InconsistencyConsumer {

    private final InconsistencyScreener inconsistencyScreener;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${hireflow.kafka.topics.inconsistency-review-completed}")
    private String inconsistencyReviewCompletedTopic;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.application-submitted}",
            groupId = "ai-screening-inconsistency-review",
            properties = {
                    "spring.json.value.default.type=com.hireflow.ai_Screening.event.ApplicationSubmittedEvent"
            }
    )
    public void consume(ApplicationSubmittedEvent event) {
        log.info("Running inconsistency review for application {}", event.getApplicationId());
        InconsistencyReviewCompletedEvent result = inconsistencyScreener.detect(event);
        kafkaTemplate.send(inconsistencyReviewCompletedTopic, event.getApplicationId(), result);
        log.info("Inconsistency review published for application {}", event.getApplicationId());
    }
}
