import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

class RetryExamples {

    // Flagged: manual loop, fixed Thread.sleep.
    void callUnsafe() {
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

    // Flagged: @Backoff with no multiplier -- fixed delay.
    @Retryable(backoff = @Backoff(delay = 1000))
    void callWithFixedBackoff() {}

    // Not flagged: real exponential multiplier.
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    void callWithExponentialBackoff() {}

    void doWork() {}
}
