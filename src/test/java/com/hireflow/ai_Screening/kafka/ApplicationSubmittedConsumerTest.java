package com.hireflow.ai_Screening.kafka;

// ApplicationSubmittedConsumer was replaced by four independent stage consumers
// that each subscribe directly to application-submitted with their own consumer group.
// See kafka/consumer/ for ResumeAnalysisConsumer, ProjectConsistencyConsumer,
// InconsistencyConsumer, and MatchSummaryConsumer and their respective tests.
