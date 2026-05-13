package com.hireflow.ai_Screening.kafka.consumer;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;
import com.hireflow.ai_Screening.service.ResumeAnalysisScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeAnalysisConsumer {

    private final ResumeAnalysisScreener resumeAnalysisScreener;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${hireflow.kafka.topics.resume-analysis-completed}")
    private String resumeAnalysisCompletedTopic;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.application-submitted}",
            groupId = "ai-screening-resume-analysis",
            properties = {
                    "spring.json.value.default.type=com.hireflow.ai_Screening.event.ApplicationSubmittedEvent"
            }
    )
    public void consume(ApplicationSubmittedEvent event) {
        log.info("Running resume analysis for application {}", event.getApplicationId());
        ResumeAnalysisCompletedEvent result = resumeAnalysisScreener.analyze(event);
        kafkaTemplate.send(resumeAnalysisCompletedTopic, event.getApplicationId(), result);
        log.info("Resume analysis published for application {}", event.getApplicationId());
    }
}
