# REST API & HTTP Virtual Threads Server — THZ-LANG

This document describes the embedded high-throughput HTTP server powered by Java 25 Virtual Threads.

---

## 1. HTTP Server Features

- **Virtual Threads (Loom):** Handles tens of thousands of concurrent client requests per second.
- **RESTful Endpoints:** Direct JSON serialization of THZ domain entities adhering to RFC 8259.
