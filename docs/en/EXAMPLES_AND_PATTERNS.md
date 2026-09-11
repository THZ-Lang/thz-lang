# Canonical Examples & Design Patterns — THZ-LANG

This document provides canonical code patterns and idiomatic implementations in **THZ-LANG** using the English (`LANGUAGE: en-US`) dialect.

---

## 1. Batch Billing & Invoicing

```thz
# LANGUAGE: en-US
PROGRAM BatchBilling

ARCHITECTURE_METADATA
    DOMAIN: "Billing"
    SUBDOMAIN: "Invoicing"
    LAYER: "Domain"
    VERSION: "2.4.0"
    AUTHOR: "Lucas Thomaz"
    MAX_LATENCY_SLO: "10ms"
    COMPLIANCE: "ISO-10967", "BACEN-Res4893"
END_METADATA

STRUCTURE Item
    description : TEXT
    unitPrice : DECIMAL(18, 2)
    quantity : INTEGER
END_STRUCTURE

BUSINESS_RULE CalculateInvoice
    INPUT_CONTRACT
        REQUIRES unitPrice > 0.00
        REQUIRES quantity > 0
    END_INPUT_CONTRACT

    OUTPUT_CONTRACT
        ENSURES total >= unitPrice
    END_OUTPUT_CONTRACT

    OPERATION Execute(item: Item) : DECIMAL(18, 2)
    BEGIN
        VARIABLE total : DECIMAL(18, 2) <- item.unitPrice * item.quantity
        IF total > 5000.00 THEN
            PRINT "High value transaction"
        END_IF
        RETURN total
    END
END_BUSINESS_RULE

END_PROGRAM
```

---

## 2. Memory Arena SIMD Vectorization

```thz
# LANGUAGE: en-US
USE_MEMORY_BLOCK FastVectorBlock
    VECTORIZE_FOR item IN dataset.records SIMD_STEP 8
        item.tax <- item.amount * 0.18
    END_FOR
END_MEMORY_BLOCK
```
