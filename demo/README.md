# Demo data for screenshots

`RetryExamples.java` — `callUnsafe`/`callWithFixedBackoff` flagged;
`callWithExponentialBackoff` not flagged.

## How to get the screenshot

1. `./gradlew runIde` from `retry-backoff-jitter-companion`, open this
   `demo/` folder as the project.
2. Full Screen, open `RetryExamples.java` — warnings should appear on
   the first two methods but not the third.
3. Screenshot with all three methods visible, save into
   `retry-backoff-jitter-companion/docs/screenshots/`. Close the
   sandbox.
