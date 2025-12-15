# Changelog

All notable changes to this project are documented in this file.

## v0.4.0
- Improved interactive vs non-interactive mode detection.
- Console output formatting improved: log prefixes are now applied per line for multi-line messages.
- Updated default IPv6 regex to a faster heuristic requiring at least two colons, reducing false positives.
- Clarified stdout/stderr behavior (sanitized output to stdout, logs and diagnostics to stderr).
- Added and expanded unit tests for configuration loading and validation.
- Documentation improvements and typo fixes.

## v0.3.0
- Initial stable pre-release version.
- Core sanitization functionality implemented.
- Configurable rules via properties file.
- CLI interface with basic logging and diagnostics.
