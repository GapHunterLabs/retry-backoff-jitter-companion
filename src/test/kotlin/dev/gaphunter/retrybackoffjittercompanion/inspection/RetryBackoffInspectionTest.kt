package dev.gaphunter.retrybackoffjittercompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RetryBackoffInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(RetryBackoffInspection::class.java)
    }

    fun `test manual retry loop with Thread sleep constant is flagged`() {
        myFixture.configureByText(
            "Client.java",
            """
            class Client {
                void call() {
                    for (int i = 0; i < 3; i++) {
                        try {
                            doWork();
                            return;
                        } catch (Exception e) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }
                void doWork() {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("thundering herd") == true })
    }

    fun `test Retryable with no Backoff at all is flagged`() {
        myFixture.configureByText(
            "Client2.java",
            """
            import org.springframework.retry.annotation.Retryable;

            class Client2 {
                @Retryable
                void call() {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("thundering herd") == true })
    }

    fun `test Retryable with a Backoff but no multiplier is flagged`() {
        myFixture.configureByText(
            "Client3.java",
            """
            import org.springframework.retry.annotation.Retryable;
            import org.springframework.retry.annotation.Backoff;

            class Client3 {
                @Retryable(backoff = @Backoff(delay = 1000))
                void call() {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("thundering herd") == true })
    }

    fun `test Retryable with a real exponential multiplier is not flagged`() {
        myFixture.configureByText(
            "Client4.java",
            """
            import org.springframework.retry.annotation.Retryable;
            import org.springframework.retry.annotation.Backoff;

            class Client4 {
                @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
                void call() {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("thundering herd") == true })
    }

    fun `test Resilience4j RetryConfig with fixed waitDuration and no intervalFunction is flagged`() {
        myFixture.configureByText(
            "Client5.java",
            """
            import java.time.Duration;

            class Client5 {
                RetryConfig config() {
                    return RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofSeconds(2))
                        .build();
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("thundering herd") == true })
    }

    fun `test Resilience4j RetryConfig with intervalFunction is not flagged`() {
        myFixture.configureByText(
            "Client6.java",
            """
            import java.time.Duration;

            class Client6 {
                RetryConfig config() {
                    return RetryConfig.custom()
                        .maxAttempts(3)
                        .intervalFunction(IntervalFunction.ofExponentialRandomBackoff())
                        .build();
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("thundering herd") == true })
    }

    fun `test a loop with Thread sleep but no catch is not flagged`() {
        myFixture.configureByText(
            "Client7.java",
            """
            class Client7 {
                void call() throws InterruptedException {
                    for (int i = 0; i < 3; i++) {
                        Thread.sleep(1000);
                    }
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("thundering herd") == true })
    }
}
