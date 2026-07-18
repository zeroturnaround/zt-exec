# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.13.0] - 2026-07-10

### Added

- `LogOutputStream.create(LineConsumer)` to build a `LogOutputStream` from a lambda ([#107](https://github.com/zeroturnaround/zt-exec/pull/107)).
- `LogOutputStream.setOutputCharset(String)` to control the charset used to decode the process output ([#89](https://github.com/zeroturnaround/zt-exec/pull/89)).
- An OSGi bundle manifest (`Bundle-SymbolicName`, `Export-Package`) in the published jar ([#85](https://github.com/zeroturnaround/zt-exec/pull/85)).
- A JPMS module descriptor for module `org.zeroturnaround.exec`, shipped as a Java 9 multi-release entry ([#106](https://github.com/zeroturnaround/zt-exec/pull/106)).

### Changed

- Raised the minimum Java runtime from 6 to 8 (bytecode target moved from 1.6 to 1.8).
- Upgraded the `slf4j-api` dependency from 1.7.2 to 1.7.32.
- Migrated the build from Maven to Gradle; releases now publish to Maven Central through the Sonatype Central Portal.

## [1.12] - 2020-09-02

### Removed

- `rebel.xml` from the release archive.

## [1.11] - 2019-07-05

### Changed

- Improved usage — eliminated cases of "log and throw".
- Use ping instead of sleep on Windows to test timeout exceptions.

### Removed

- Dependency on Apache Commons IO.

### Fixed

- Various bug fixes.

## [1.10] - 2017-08-01

### Added

- Support for flushing output after each write.
- Hooks for thread executor creation.
- Support for preserving the MDC context of the caller thread.

### Changed

- `LogOutputStream` handles line breaks like `ProcessOutput`.

## [1.9] - 2016-04-12

### Added

- `ProcessInitException` to expose the error code.
- `ProcessExecutor.checkExitValue()` for unit testing.
- Getters to `ProcessExecutor`.

### Fixed

- Using empty arguments on Windows.
- Closing stdin on Java 8.
- Redirecting `PipedInputStream`.

## [1.8] - 2015-03-17

### Added

- `ProcessExecutor.closeTimeout()`.
- `ProcessOutput.getLines()`.
- `ProcessOutput.command(Iterable)`.
- `ProcessListener.afterFinish()` and `InvalidOutputException` support.
- `Level` class.

### Changed

- Improved `Slf4jStream`.
- Improved the `TimeoutException` message.

### Fixed

- Blocking JVM shutdown.

## [1.7] - 2014-06-30

### Added

- `ProcessExecutor.stopper(ProcessStopper)` to customize stopping on timeout or cancellation.
- `ProcessExecutor.setMessageLogger()` to customize the log message level.
- `ProcessExecutor.executeNoTimeout()` to avoid catching `TimeoutException`.
- `ProcessExecutor.environment(String, String)` for convenience.
- get-prefixed `StartedProcess` methods (the old ones are deprecated).

### Changed

- Improved logging and error handling.

### Fixed

- `ProcessExecutor.commandSplit()`.

## [1.6] - 2014-01-30

### Added

- `ProcessExecutor.redirectInput()`.
- get-prefixed variants of some methods (the old ones are deprecated).

### Changed

- Improved SLF4J logging support.
- Improved adding and removing destroyers and listeners.
- `InvalidExitValueException` now includes the process output when it was read.

## [1.5] - 2013-10-14

### Changed

- Any exit code is now allowed by default (use `exitValueNormal()` to allow only zero).

### Fixed

- Starting processes on JVM shutdown.
- Supporting Java 1.5.

[Unreleased]: https://github.com/zeroturnaround/zt-exec/compare/v1.13.0...HEAD
[1.13.0]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.12...v1.13.0
[1.12]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.11...zt-exec-1.12
[1.11]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.10...zt-exec-1.11
[1.10]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.9...zt-exec-1.10
[1.9]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.8...zt-exec-1.9
[1.8]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.7...zt-exec-1.8
[1.7]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.6...zt-exec-1.7
[1.6]: https://github.com/zeroturnaround/zt-exec/compare/zt-exec-1.5...zt-exec-1.6
[1.5]: https://github.com/zeroturnaround/zt-exec/releases/tag/zt-exec-1.5
