package dev.gaphunter.retrybackoffjittercompanion.model

import com.intellij.psi.PsiElement

enum class RetryPatternKind {
    /** A manual `for`/`while` retry loop whose `catch` calls `Thread.sleep(constant)`. */
    MANUAL_LOOP,

    /** Spring `@Retryable` with no `@Backoff`, or one with no real `multiplier` (> 1). */
    SPRING_RETRYABLE,

    /** Resilience4j `RetryConfig` built with a fixed `.waitDuration(...)` and no `.intervalFunction(...)`. */
    RESILIENCE4J,
}

/** One retry mechanism with a fixed interval instead of exponential backoff + jitter -- a real "thundering herd" risk. */
data class RetryBackoffHit(val anchor: PsiElement, val kind: RetryPatternKind)
