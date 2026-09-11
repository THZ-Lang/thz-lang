# ADR-005 — Swing+FlatLaf vs WebView2 for UI Archetypes

**Date:** 2025-08-25 · **Status:** Accepted · **Context:** UI rendering engine for `SCREEN` (`.thzui`) and Desktop IDE.

## Context

We evaluated graphical rendering engines for cross-platform desktop execution: Native Win32 GDI, Swing with FlatLaf modern styling, and WebView2 (Chromium).

## Decision

- **Desktop IDE:** Swing + FlatLaf modern Dark/Light theme for `thz gui`.
- **Declarative Screens (`.thzui`):** HTML5 / Glassmorphism renderer and WebView2 bridge for declarative screens.
