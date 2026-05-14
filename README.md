# HireFlow AI Screening Service

Stateless screening worker for HireFlow. It consumes application-submitted events from Kafka, runs independent screening stages, and publishes structured results back to Kafka for the application service.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Messaging | Spring Kafka |
| AI provider | Gemini Chat Completions |
| Build | Maven |
| Tests | JUnit 5, Mockito, AssertJ |

## How It Works

Each screening stage has two implementations:

| Implementation | When active | Behavior |
|---|---|---|
| `Basic*` | Always registered | Deterministic keyword/rule scoring, used as fallback |
| `Gemini*` | Always primary | Sends structured prompts to Gemini and falls back to `Basic*` if the provider call fails |

The client uses Gemini's chat-completions endpoint and all provider configuration is Gemini-only.

## Kafka Flow

All four consumers read from `hireflow.application.submitted.v1` using separate consumer groups, so each stage receives a full copy of every submitted application.

| Stage | Consumer group | Output topic |
|---|---|---|
| Resume analysis | `ai-screening-resume-analysis` | `hireflow.screening.resume.v1` |
| Project consistency | `ai-screening-project-consistency` | `hireflow.screening.project.v1` |
| Inconsistency review | `ai-screening-inconsistency-review` | `hireflow.screening.inconsistency.v1` |
| Match summary | `ai-screening-match-summary` | `hireflow.screening.completed.v1` |

## Configuration

`src/main/resources/application.properties` reads from environment variables. For local development, create an ignored `src/main/resources/env.properties` from `env.example.properties`.

Required Kafka variables:

```properties
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_API_KEY=replace-me
KAFKA_API_SECRET=replace-me
APPLICATION_SUBMITTED_TOPIC=hireflow.application.submitted.v1
RESUME_ANALYSIS_COMPLETED_TOPIC=hireflow.screening.resume.v1
PROJECT_CONSISTENCY_COMPLETED_TOPIC=hireflow.screening.project.v1
INCONSISTENCY_REVIEW_COMPLETED_TOPIC=hireflow.screening.inconsistency.v1
SCREENING_COMPLETED_TOPIC=hireflow.screening.completed.v1
```

Gemini variables:

```properties
GEMINI_API_KEY=replace-me
GEMINI_MODEL=gemini-2.5-flash
```

`GEMINI_API_KEY` is required. `GOOGLE_API_KEY` is also accepted as a fallback. If no key is present, startup fails fast.

Default Gemini endpoint:

```text
https://generativelanguage.googleapis.com/v1beta/openai
```

## Running

```bash
mvn spring-boot:run
```

The service starts on port `8082` by default and exposes no REST workflow endpoints. Work is event-driven through Kafka.

## Testing

```bash
mvn test
```

For a faster compile check:

```bash
mvn -DskipTests compile
```

## Production Notes

Do not commit real API keys or Kafka credentials. Use deployment environment variables or your platform's secret manager.

`src/main/resources/env.properties` is ignored by Git and is intended only for local development.
