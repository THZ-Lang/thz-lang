# Troubleshooting & FAQ — THZ-LANG

Common diagnostics, solutions, and frequently asked questions for THZ-LANG.

---

## 1. Common Issues

### "Dialect Purity Violation"
- **Cause:** Using Portuguese keywords inside a file with `LANGUAGE: en-US` or vice-versa.
- **Solution:** Change the keywords to the active dialect or update line 1 header directive.

### "Currency Mismatch Exception"
- **Cause:** Performing direct arithmetic between different ISO 4217 currencies (e.g. `BRL` and `USD`).
- **Solution:** Apply explicit exchange rate conversion before addition or subtraction.
