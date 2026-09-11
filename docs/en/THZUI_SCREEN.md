# Declarative UI DSL & Screen Archetype (`.thzui`) — THZ-LANG

This document describes the declarative UI subsystem (`.thzui` / `SCREEN` archetype) in THZ-LANG.

---

## 1. Declarative UI Syntax

```thz
# LANGUAGE: en-US
SCREEN CustomerDashboard

ARCHITECTURE_METADATA
    DOMAIN: "CustomerSuccess"
    LAYER: "UI"
    VERSION: "2.4.0"
END_METADATA

PROCEDURE OnLoad()
BEGIN
    PRINT "Dashboard initialized"
END

END_SCREEN
```
