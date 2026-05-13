# HireFlow — AI Screening Service

> Stateless AI screening worker for the HireFlow platform. Consumes application-submitted events from Kafka, runs four parallel screening stages, and publishes structured results back to Kafka for the application service to consume.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Responsibility](#responsibility)
3. [Architecture](#architecture)
4. [Screening Stages](#screening-stages)
5. [OpenAI vs Deterministic Mode](#openai-vs-deterministic-mode)
6. [Kafka Topics](#kafka-topics)
7. [Project Structure](#project-structure)
8. [Getting Started](#getting-started)
9. [Configuration](#configuration)
10. [Running the Service](#running-the-service)
11. [Testing](#testing)

---

## Tech Stack

| Layer        | Technology                                         |
|--------------|----------------------------------------------------|
| Language     | Java 21                                            |
| Framework    | Spring Boot 4.0.6                                  |
| Messaging    | Spring Kafka (consumer + producer)                 |
| AI           | OpenAI Chat Completions API via Spring `RestClient`|
| Build        | Maven                                              |
| Testing      | JUnit 5, Mockito, AssertJ                          |

---

## Responsibility

This service is a **stateless processing worker**. It owns no database and holds no persistent state. Its job is:

1. Consume `ApplicationSubmittedEvent` messages from Kafka.
2. Run four independent screening analyses in parallel (one per consumer group).
3. Publish each result to its own Kafka topic for the application service to pick up.

If the service goes down, Kafka holds the unconsumed messages. When it restarts, each consumer group resumes from its last committed offset — no messages are lost.

---

## Architecture

```
hireflow (Application Service)
         │
         │  publishes ApplicationSubmittedEvent to Kafka
         ▼
[hireflow.application.submitted.v1]
         │
    ┌────┼───────────────────────────┐
    │    │                           │
    ▼    ▼                           ▼
ResumeAnalysis  ProjectConsistency  Inconsistency  MatchSummary
Consumer        Consumer            Consumer       Consumer
(group: resume) (group: project)    (group: incon) (group: match)
    │                │                   │              │
    ▼                ▼                   ▼              ▼
screening.       screening.         screening.      screening.
resume.v1        project.v1         inconsistency.v1 completed.v1
    │                │                   │              │
    └────────────────┴───────────────────┘──────────────┘
                         │
                         ▼
              hireflow (Application Service)
              consumes all 4 result topics
```

Each consumer uses a **different Kafka consumer group** on the same `application-submitted` topic. Kafka delivers a full, independent copy of every event to each group. A failure in one stage has zero effect on the other three.

---

## Screening Stages

### 1. Resume Analysis
**Consumer group:** `ai-screening-resume-analysis`  
**Publishes to:** `hireflow.screening.resume.v1`

Compares the applicant's skill list against the job's required skills. Produces a 0–100 score, a candidate-facing explanation, and an internal HR review note.

### 2. Project Consistency
**Consumer group:** `ai-screening-project-consistency`  
**Publishes to:** `hireflow.screening.project.v1`

Checks whether the applicant's resume summary and interview answers provide evidence of the required skills in a project or work context. Produces a consistency score and narrative.

### 3. Inconsistency Review
**Consumer group:** `ai-screening-inconsistency-review`  
**Publishes to:** `hireflow.screening.inconsistency.v1`

Looks for contradictions between the applicant's claimed skills, resume summary, and their answers to role-specific questions. Flags inconsistencies for human review.

### 4. Match Summary
**Consumer group:** `ai-screening-match-summary`  
**Publishes to:** `hireflow.screening.completed.v1`

Produces the final rolled-up screening result: `matchPercentage`, `matchedSkills`, `unmatchedSkills`, and an `aiNarrativeSummary`. This is the event the application service uses to advance the application stage.

---

## OpenAI vs Deterministic Mode

Each screening stage ships with two implementations:

| Implementation | Bean condition | Behaviour |
|---|---|---|
| `Basic*` (e.g. `BasicResumeAnalysisScreener`) | Always registered | Pure keyword matching + rule-based scoring. No external calls. Fully deterministic. |
| `OpenAi*` (e.g. `OpenAiResumeAnalysisScreener`) | `@ConditionalOnProperty(prefix = "hireflow.ai.openai", name = "enabled", havingValue = "true")` + `@Primary` | Sends a structured prompt to OpenAI Chat Completions. Falls back to the `Basic*` implementation if the API call fails. |

Setting `OPENAI_ENABLED=false` (the default) means only the `Basic*` beans are active. Setting it to `true` activates all four `OpenAi*` beans as `@Primary`, which shadow the `Basic*` beans — the `Basic*` beans remain in the context as explicit fallback dependencies inside the `OpenAi*` constructors.

---

## Kafka Topics

| Topic | Direction | Purpose |
|---|---|---|
| `hireflow.application.submitted.v1` | Consumed (×4 groups) | One copy delivered to each of the 4 consumer groups |
| `hireflow.screening.resume.v1` | Produced | `ResumeAnalysisCompletedEvent` |
| `hireflow.screening.project.v1` | Produced | `ProjectConsistencyCompletedEvent` |
| `hireflow.screening.inconsistency.v1` | Produced | `InconsistencyReviewCompletedEvent` |
| `hireflow.screening.completed.v1` | Produced | `ScreeningCompletedEvent` (final result) |

---

## Project Structure

```
src/main/java/com/hireflow/ai_Screening/
├── config/
│   ├── OpenAiConfig.java          — RestClient bean wired to OpenAI base URL + auth
│   ├── OpenAiProperties.java      — @ConfigurationProperties for hireflow.ai.openai.*
│   └── ObjectMapperConfig.java    — shared Jackson ObjectMapper bean
├── event/
│   ├── ApplicationSubmittedEvent.java        — inbound event (consumed from Kafka)
│   ├── ApplicationSubmittedAnswer.java       — embedded answer DTO inside submitted event
│   ├── ResumeAnalysisCompletedEvent.java     — outbound: stage 1 result
│   ├── ProjectConsistencyCompletedEvent.java — outbound: stage 2 result
│   ├── InconsistencyReviewCompletedEvent.java— outbound: stage 3 result
│   └── ScreeningCompletedEvent.java          — outbound: final match summary
├── kafka/
│   ├── ApplicationSubmittedConsumer.java     — replaced; stub comment only
│   └── consumer/
│       ├── ResumeAnalysisConsumer.java       — group: ai-screening-resume-analysis
│       ├── ProjectConsistencyConsumer.java   — group: ai-screening-project-consistency
│       ├── InconsistencyConsumer.java        — group: ai-screening-inconsistency-review
│       └── MatchSummaryConsumer.java         — group: ai-screening-match-summary
├── restclient/
│   ├── OpenAiChatClient.java                 — interface: completeJson(system, user)
│   └── impl/
│       ├── SpringOpenAiChatClient.java       — RestClient-based implementation
│       └── OpenAiChatException.java          — thrown on non-2xx or malformed response
└── service/
    ├── ResumeAnalysisScreener.java           — interface
    ├── ProjectConsistencyScreener.java       — interface
    ├── InconsistencyScreener.java            — interface
    ├── MatchSummariser.java                  — interface
    └── impl/
        ├── ScreeningAnalysisSupport.java     — shared static helpers (normalize, safeList…)
        ├── BasicResumeAnalysisScreener.java
        ├── BasicProjectConsistencyScreener.java
        ├── BasicInconsistencyScreener.java
        ├── BasicMatchSummariser.java
        ├── OpenAiResumeAnalysisScreener.java
        ├── OpenAiProjectConsistencyScreener.java
        ├── OpenAiInconsistencyScreener.java
        ├── OpenAiMatchSummariser.java
        ├── OpenAiPromptFactory.java          — builds structured user prompts
        └── OpenAiResponseSupport.java        — parses + validates JSON responses
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Kafka broker running (default: `localhost:9092`)
- hireflow Application Service running and publishing events
- (Optional) OpenAI API key for AI-powered screening

### Clone

```bash
git clone <repo-url>
cd ai-Screening
```

---

## Configuration

Copy `env.properties` from the example and fill in your values:

```properties
AI_SCREENING_SERVER_PORT=8082

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
AI_SCREENING_KAFKA_GROUP_ID=ai-screening-service

APPLICATION_SUBMITTED_TOPIC=hireflow.application.submitted.v1
RESUME_ANALYSIS_COMPLETED_TOPIC=hireflow.screening.resume.v1
PROJECT_CONSISTENCY_COMPLETED_TOPIC=hireflow.screening.project.v1
INCONSISTENCY_REVIEW_COMPLETED_TOPIC=hireflow.screening.inconsistency.v1
SCREENING_COMPLETED_TOPIC=hireflow.screening.completed.v1

# Set to true and provide an API key to use real AI screening
OPENAI_ENABLED=false
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o-mini
```

### Enabling OpenAI

Set `OPENAI_ENABLED=true` and provide a valid `OPENAI_API_KEY`. The `OpenAi*` screeners become `@Primary` and take over from the `Basic*` screeners. If an OpenAI call fails, the service automatically falls back to the deterministic `Basic*` result for that stage — no manual intervention required.

---

## Running the Service

```bash
mvn spring-boot:run
```

The service starts on port `8082` (default). It exposes no REST endpoints — all work is event-driven via Kafka.

---

## Testing

```bash
mvn test
```

### Test coverage

| Class | Tests |
|---|---|
| `ResumeAnalysisConsumerTest` | Screener called and result published to correct topic; exception propagates without publishing |
| `ProjectConsistencyConsumerTest` | Screener called and result published to correct topic; exception propagates without publishing |
| `InconsistencyConsumerTest` | Screener called and result published to correct topic; exception propagates without publishing |
| `MatchSummaryConsumerTest` | Summariser called and result published to correct topic; exception propagates without publishing |
| `BasicMatchSummariserTest` | Skill match percentage; all matched; none matched; empty skills defaults to 50; matched/unmatched lists correct |
| `OpenAiResumeAnalysisScreenerTest` | OpenAI response parsed correctly; falls back to Basic on API failure |
