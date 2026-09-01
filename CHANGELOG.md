<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Retry Without Backoff+Jitter Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning on a retry mechanism with a fixed interval instead of
  exponential backoff + jitter -- covers three shapes: a manual
  loop's `Thread.sleep(constant)`, Spring `@Retryable` with no real
  exponential `@Backoff`, and Resilience4j's `RetryConfig` with a
  fixed `.waitDuration(...)` and no `.intervalFunction(...)`.

[Unreleased]: https://github.com/GapHunterLabs/retry-backoff-jitter-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/retry-backoff-jitter-companion/commits/0.1.0
