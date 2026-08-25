# ADR-004 — GraalVM Native Image vs jpackage for Tooling Distribution

**Date:** 2025-08-25 · **Status:** Accepted · **Context:** Distribution strategy for `thz` CLI and Desktop IDE.

## Context

`thz-cli-jvm` and `thz-gui-jvm` leverage Swing with FlatLaf themes. GraalVM `native-image` requires extensive reflection configuration, whereas JDK `jpackage` creates self-contained native bundles with stripped runtime images via `jlink`.

## Decision

- **Primary Packaging:** `jpackage` for standard desktop distributions (`dist/thz/`).
- **Optional Native Image:** GraalVM native image build (`dist/bin/thz.exe`) for fast-startup CLI workflows.
