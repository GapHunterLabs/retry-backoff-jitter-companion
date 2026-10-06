<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Retry Without Backoff+Jitter Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Warning on a retry mechanism with a fixed interval instead of
  exponential backoff + jitter -- covers three shapes: a manual
  loop's `Thread.sleep(constant)`, Spring `@Retryable` with no real
  exponential `@Backoff`, and Resilience4j's `RetryConfig` with a
  fixed `.waitDuration(...)` and no `.intervalFunction(...)`.

[Unreleased]: https://github.com/GapHunterLabs/retry-backoff-jitter-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/retry-backoff-jitter-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/retry-backoff-jitter-companion/commits/0.1.0
