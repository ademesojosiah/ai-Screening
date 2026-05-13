package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import com.hireflow.ai_Screening.service.ProjectConsistencyScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectConsistencyConsumer {

    private final ProjectConsistencyScreener projectConsistencyScreener;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${hireflow.kafka.topics.project-consistency-completed}")
    private String projectConsistencyCompletedTopic;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.application-submitted}",
            groupId = "ai-screening-project-consistency",
            properties = {
                    "spring.json.value.default.type=com.hireflow.ai_Screening.event.ApplicationSubmittedEvent"
            }
    )
    public void consume(ApplicationSubmittedEvent event) {
        log.info("Running project consistency check for application {}", event.getApplicationId());
        ProjectConsistencyCompletedEvent result = projectConsistencyScreener.score(event);
        kafkaTemplate.send(projectConsistencyCompletedTopic, event.getApplicationId(), result);
        log.info("Project consistency published for application {}", event.getApplicationId());
    }
}
