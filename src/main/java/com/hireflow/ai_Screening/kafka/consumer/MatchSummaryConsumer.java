package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.MatchSummariser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSummaryConsumer {

    private final MatchSummariser matchSummariser;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${hireflow.kafka.topics.screening-completed}")
    private String screeningCompletedTopic;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.application-submitted}",
            groupId = "ai-screening-match-summary",
            properties = {
                    "spring.json.value.default.type=com.hireflow.ai_Screening.event.ApplicationSubmittedEvent"
            }
    )
    public void consume(ApplicationSubmittedEvent event) {
        log.info("Running match summary for application {}", event.getApplicationId());
        ScreeningCompletedEvent result = matchSummariser.summarise(event);
        kafkaTemplate.send(screeningCompletedTopic, event.getApplicationId(), result);
        log.info("Match summary published for application {}", event.getApplicationId());
    }
}
