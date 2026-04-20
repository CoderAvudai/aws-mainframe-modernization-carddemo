# CardDemo Bill Payment Service (Java 21 / Spring Boot 3)

This module is a faithful modernization of the online CICS COBOL program
`COBIL00C.cbl` (the **Bill Payment** transaction of the CardDemo application).
Every business rule extracted from the COBOL source is preserved — see
[`docs/COBIL00C_SPEC.md`](../../docs/COBIL00C_SPEC.md) for the full specification
and the paragraph-level traceability table.

## Build & Test

```bash
cd java/carddemo-billpay
mvn -B test
```

Requires **JDK 21**.

## Run

```bash
mvn spring-boot:run
```

## REST API

The COBOL program is a pseudo-conversational 3270/BMS transaction; this service
exposes the same state machine over HTTP/JSON.

### `POST /api/billpay`

Body:

```json
{ "accountId": "00000000010", "confirm": "Y" }
```

`confirm` values (case-insensitive):

| Value       | Behaviour                                                     |
|-------------|---------------------------------------------------------------|
| `"Y"`       | Execute the payment (pay balance in full, write transaction). |
| `"N"`       | Cancel — clear the screen, no message.                        |
| `null`/`""` | Inquiry mode — return current balance and prompt for confirm. |
| anything else | Validation error `Invalid value. Valid values are (Y/N)...`|

### `POST /api/billpay/clear`

Resets the screen (PF4 equivalent).

## Scope

This module implements **only** the COBIL00C program. It does not model:

* BMS screen rendering or 3270 attribute bytes (CICS presentation layer).
* AID key handling beyond the JSON equivalent of ENTER, PF3, PF4.
* Program XCTL navigation between CardDemo programs (PF3 returns a navigation
  hint to the caller; it does not wire up COMEN01C or COSGN00C).

These concerns are out of scope for a single-program modernization and would be
handled by the enclosing Spring Boot application in a full port of CardDemo.
