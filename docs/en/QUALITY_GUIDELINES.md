# Engineering Quality Guidelines & Testing Standards — THZ-LANG

This document defines the code quality standards, static verification constraints, and testing criteria mandatory across the THZ-LANG ecosystem.

---

## 1. Quality Standards

- **Zero Regression Rule:** All pull requests must maintain 100% passing test execution across JVM, Native, and Web tooling.
- **Contract Verification:** All corporate business rules must declare formal `REQUIRES` and `ENSURES` clauses with explicit boundary constraints.
- **Pure Arithmetic:** Strict adherence to ISO/IEC 10967 with zero floating-point arithmetic in monetary calculation paths.
- **Deterministic Versioning:** All build outputs and metadata must strictly adhere to SemVer 2.0.0.
