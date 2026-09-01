# Retry Without Backoff+Jitter Companion

Warning on a retry mechanism with a FIXED interval between attempts
instead of exponential backoff + jitter: a manual `for`/`while` retry
loop that `Thread.sleep(constant)`s in its `catch`, Spring `@Retryable`
with no real exponential `@Backoff`, or Resilience4j's `RetryConfig`
built with a fixed `.waitDuration(...)` and no `.intervalFunction(...)`.

## Why it exists

The "thundering herd" anti-pattern -- synchronized retries from
multiple instances of a service saturate the downstream exactly at the
same instant, documented in distributed-systems literature (AWS
Architecture Blog, Google SRE Book) as a real cause of cascading
availability incidents. JOptimize (Marketplace, confirmed) flags only
the ABSENCE of any retry on an HTTP call -- this is a deeper
granularity: the QUALITY of a retry mechanism that already exists.

## Why built this way

Covers three known real shapes, each with its own recognition rule:

- **Manual loop**: a `for`/`while` whose `catch` calls
  `Thread.sleep(<constant literal>)` -- a computed/variable delay is
  never flagged, since it could already be backoff logic.
- **Spring `@Retryable`**: flags when there's no `@Backoff` at all
  (Spring's own default is an immediate retry with zero delay) or one
  with no real `multiplier` (> 1, i.e. actually exponential).
- **Resilience4j**: flags a `RetryConfig` chain with `.waitDuration(...)`
  and no `.intervalFunction(...)` anywhere in the same chain.

## v0.1 scope — stated honestly, not exhaustively

Only these three known forms -- never analyzes a fully custom retry
implementation with none of these recognizable signals.

## Usage

Open any Java file. A retry mechanism with a fixed interval (in any of
the three known shapes) shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
