package com.hireflow.ai_Screening.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.evidenceSupportsSkill;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lives in the same package so we can call the package-private helpers directly.
 * `evidenceSupportsSkill` is the core token-matching primitive that powers both
 * the project consistency and inconsistency screeners.
 */
class ScreeningAnalysisSupportTest {

    @Test
    @DisplayName("Should match a single-token skill when its token appears in the evidence")
    void matchesSingleTokenSkill() {
        assertThat(evidenceSupportsSkill("Backend engineer with Java production experience", "Java")).isTrue();
    }

    @Test
    @DisplayName("Should not match Java when the evidence only mentions JavaScript")
    void respectsWordBoundary() {
        assertThat(evidenceSupportsSkill("Full-stack developer with JavaScript", "Java")).isFalse();
    }

    @Test
    @DisplayName("Should require every token of a multi-word skill to appear in the evidence")
    void multiWordSkillRequiresAllTokens() {
        assertThat(evidenceSupportsSkill("Built REST APIs with Spring Boot", "Spring Boot")).isTrue();
        assertThat(evidenceSupportsSkill("Expert in Spring and Hibernate", "Spring Boot")).isFalse();
    }

    @Test
    @DisplayName("Should be case-insensitive on both evidence and skill")
    void caseInsensitive() {
        assertThat(evidenceSupportsSkill("BACKEND ENGINEER WITH JAVA", "java")).isTrue();
        assertThat(evidenceSupportsSkill("backend engineer with java", "JAVA")).isTrue();
    }

    @Test
    @DisplayName("Should return false when either evidence or skill is blank")
    void blankInputsReturnFalse() {
        assertThat(evidenceSupportsSkill(null, "Java")).isFalse();
        assertThat(evidenceSupportsSkill("", "Java")).isFalse();
        assertThat(evidenceSupportsSkill("   ", "Java")).isFalse();
        assertThat(evidenceSupportsSkill("resume text", null)).isFalse();
        assertThat(evidenceSupportsSkill("resume text", "")).isFalse();
    }

    @Test
    @DisplayName("Should fall back to substring match for skills shorter than the token threshold (e.g. 'Go')")
    void shortSkillFallback() {
        // "go" is only 2 chars so significantTokens drops it; the substring fallback handles it.
        assertThat(evidenceSupportsSkill("Backend developer with Go and Java experience", "Go")).isTrue();
        assertThat(evidenceSupportsSkill("Backend developer with Java only", "Go")).isFalse();
    }

    @Test
    @DisplayName("Should keep tokens with allowed special characters (+, #, .)")
    void preservesSpecialCharTokens() {
        // "c++" and "c#" must survive significantTokens' filter.
        assertThat(evidenceSupportsSkill("Systems programmer fluent in c++", "C++")).isTrue();
        assertThat(evidenceSupportsSkill("Backend developer using c# on .net", "C#")).isTrue();
    }
}
