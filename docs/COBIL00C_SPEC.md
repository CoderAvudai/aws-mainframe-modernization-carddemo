# COBIL00C — Business Rule Specification

**Source file:** [`app/cbl/COBIL00C.cbl`](../app/cbl/COBIL00C.cbl)
**Transaction ID:** `CB00`
**Program ID:** `COBIL00C`
**Function (per source header):** *Bill Payment — Pay account balance in full
and record a transaction for the online bill payment.*
**Related copybooks:**
[`CVACT01Y`](../app/cpy/CVACT01Y.cpy) (ACCOUNT-RECORD, 300 bytes),
[`CVACT03Y`](../app/cpy/CVACT03Y.cpy) (CARD-XREF-RECORD, 50 bytes),
[`CVTRA05Y`](../app/cpy/CVTRA05Y.cpy) (TRAN-RECORD, 350 bytes),
[`COCOM01Y`](../app/cpy/COCOM01Y.cpy) (CARDDEMO-COMMAREA),
[`COBIL00`](../app/cpy-bms/COBIL00.CPY) (BMS map).
**VSAM datasets accessed:** `ACCTDAT` (account master, primary key = ACCT-ID),
`CXACAIX` (card-xref alternate index, key = XREF-ACCT-ID), `TRANSACT`
(transaction journal, key = TRAN-ID).

> **Note on naming.** Although the task statement refers to COBIL00C as
> *"statement generation"*, the program's own header and logic clearly
> implement **Bill Payment**. Statement generation in CardDemo is performed by
> the batch program `CBSTM03A`. This specification describes COBIL00C as it is
> actually written.

---

## 1. Executive summary

COBIL00C is a pseudo-conversational CICS online transaction. The user enters
an account ID; the program looks up the account balance, displays it, and asks
for a Y/N confirmation. On confirmation (`Y`), the program:

1. Looks up the card number associated with the account via the `CXACAIX`
   alternate-index file.
2. Generates the next sequential transaction ID by browsing `TRANSACT`
   backwards from `HIGH-VALUES`.
3. Writes a new `TRAN-RECORD` representing the full-balance bill payment.
4. Rewrites the `ACCOUNT-RECORD` with the balance reduced by the paid amount
   (i.e., the balance becomes zero when paying the full current balance).
5. Displays a "Payment successful" message including the new transaction ID.

---

## 2. Scenario catalogue

Each scenario lists the preconditions, the trigger, the rules applied (with
`R#` identifiers used by the traceability table in §4), and the expected
observable outcome.

### S1 — Cold entry (no COMMAREA)

*Trigger:* `EIBCALEN = 0` at entry to `MAIN-PARA`.

* **R1.** The program MUST redirect control to the sign-on program
  (`CDEMO-TO-PROGRAM` := `COSGN00C`) and `XCTL` via `RETURN-TO-PREV-SCREEN`.
* No screen is sent by COBIL00C itself in this path.

### S2 — First entry from a peer program (initial display)

*Trigger:* `EIBCALEN > 0` and `CDEMO-PGM-REENTER` is false.

* **R2.** Set the re-enter flag so subsequent invocations are treated as
  conversational turns.
* **R3.** Clear the output map (`COBIL0AO := LOW-VALUES`) and position the
  cursor on the Account-ID field (`ACTIDINL := -1`).
* **R4.** If the caller passed a pre-selected transaction (non-blank
  `CDEMO-CB00-TRN-SELECTED` in COMMAREA), copy it into the account-ID input
  field and invoke `PROCESS-ENTER-KEY` as if the user had pressed ENTER.
* **R5.** Send the bill-payment screen.

### S3 — Conversational turn (`CDEMO-PGM-REENTER` true)

*Trigger:* Any AID key after the first display.

The AID (attention-identifier) key value in `EIBAID` drives routing:

| AID            | Rule  | Behaviour                                                     |
|----------------|-------|---------------------------------------------------------------|
| `DFHENTER`     | R6    | Invoke `PROCESS-ENTER-KEY`.                                   |
| `DFHPF3`       | R7    | Return to previous program: target = `CDEMO-FROM-PROGRAM`; if that is blank/low-values use `COMEN01C` (main menu). Transfer via `XCTL`. |
| `DFHPF4`       | R8    | `CLEAR-CURRENT-SCREEN` — reinitialise all input fields, position cursor on Account ID, re-display. |
| anything else  | R9    | Error: set `WS-MESSAGE` = `CCDA-MSG-INVALID-KEY` and redisplay the screen. |

### S4 — Final return

*After every full turn:* **R10.** `EXEC CICS RETURN TRANSID('CB00')
COMMAREA(CARDDEMO-COMMAREA)` — pseudo-conversational pattern keeps the same
transaction ID alive for the next ENTER.

### S5 — ENTER with blank account ID

*Trigger:* `PROCESS-ENTER-KEY` invoked and `ACTIDINI` is SPACES or LOW-VALUES.

* **R11.** Error: `WS-MESSAGE := "Acct ID can NOT be empty..."`, cursor to
  Account-ID (`ACTIDINL := -1`), re-display.
* No I/O against any VSAM dataset is performed.

### S6 — ENTER with invalid CONFIRM value

*Trigger:* Account ID present, `CONFIRMI` not in `{Y, y, N, n, SPACES,
LOW-VALUES}`.

* **R12.** Error: `WS-MESSAGE := "Invalid value. Valid values are (Y/N)..."`,
  cursor to CONFIRM field (`CONFIRML := -1`), re-display.
* No I/O against any VSAM dataset is performed.

### S7 — Inquiry (no confirmation yet)

*Trigger:* Account ID present, CONFIRM is blank / LOW-VALUES.

* **R13.** `MOVE ACTIDINI TO ACCT-ID, XREF-ACCT-ID`.
* **R14.** Read `ACCTDAT` with UPDATE intent (R24–R26 govern errors).
* **R15.** Display the current balance on the map (`ACCT-CURR-BAL →
  CURBALI`).
* **R16.** If `ACCT-CURR-BAL <= 0` (and account ID is non-blank — the guard is
  explicit in `PROCESS-ENTER-KEY`): error `WS-MESSAGE := "You have nothing to
  pay..."`, cursor to Account-ID, re-display. **Do not proceed to payment.**
* **R17.** Otherwise re-display the map with the balance and prompt
  `WS-MESSAGE := "Confirm to make a bill payment..."`, cursor on CONFIRM.

### S8 — Cancellation (`N`)

*Trigger:* CONFIRM = `N` or `n`.

* **R18.** `CLEAR-CURRENT-SCREEN` (re-initialise fields, no error message).
* **R19.** Set the error flag so the rest of the payment flow is skipped (no
  VSAM access happens). No "cancelled" message is sent — the screen simply
  resets.

### S9 — Successful bill payment (`Y`)

*Trigger:* CONFIRM = `Y` or `y`, account exists, balance > 0.

1. **R20.** Read `ACCTDAT` with UPDATE intent (holds the record for REWRITE).
2. **R21.** Read `CXACAIX` with key `XREF-ACCT-ID` to retrieve the associated
   card number (`XREF-CARD-NUM`).
3. **R22.** Generate the next transaction ID:
   * `MOVE HIGH-VALUES TO TRAN-ID`.
   * `STARTBR` on `TRANSACT` with `TRAN-ID` as RID.
   * `READPREV` once — on `NORMAL` use `TRAN-ID` from the record; on `ENDFILE`
     set `TRAN-ID := 0` (first transaction in the file).
   * `ENDBR`.
   * `WS-TRAN-ID-NUM := TRAN-ID + 1`.
3. **R23.** Build a new `TRAN-RECORD` with these **fixed** values (case /
   length per copybook) and the dynamic fields listed below:

   | Field               | Value                              |
   |---------------------|------------------------------------|
   | `TRAN-ID`           | `WS-TRAN-ID-NUM` (16 digits)       |
   | `TRAN-TYPE-CD`      | `"02"`                             |
   | `TRAN-CAT-CD`       | `2`                                |
   | `TRAN-SOURCE`       | `"POS TERM"`                       |
   | `TRAN-DESC`         | `"BILL PAYMENT - ONLINE"`          |
   | `TRAN-AMT`          | `ACCT-CURR-BAL` (amount at time of read) |
   | `TRAN-MERCHANT-ID`  | `999999999`                        |
   | `TRAN-MERCHANT-NAME`| `"BILL PAYMENT"`                   |
   | `TRAN-MERCHANT-CITY`| `"N/A"`                            |
   | `TRAN-MERCHANT-ZIP` | `"N/A"`                            |
   | `TRAN-CARD-NUM`     | `XREF-CARD-NUM`                    |
   | `TRAN-ORIG-TS`      | current timestamp (see R27)        |
   | `TRAN-PROC-TS`      | current timestamp (same as ORIG)   |

4. **R24.** `WRITE` to `TRANSACT`.
5. **R25.** `COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT` (i.e., the new
   balance is exactly zero because the paid amount equals the starting
   balance).
6. **R26.** `REWRITE` the `ACCOUNT-RECORD`.
7. **R27.** On successful write, clear the screen fields, set the message
   colour to `DFHGREEN`, and build the success message
   `"Payment successful. Your Transaction ID is <tran-id>."`, then send the
   screen.

### S10 — File / DB error paths

| COBOL location                  | VSAM RESP            | Rule  | User message                              | Cursor target |
|---------------------------------|----------------------|-------|-------------------------------------------|---------------|
| `READ-ACCTDAT-FILE`             | `NOTFND`             | R28   | `"Account ID NOT found..."`               | `ACTIDINL`    |
| `READ-ACCTDAT-FILE`             | any other non-normal | R29   | `"Unable to lookup Account..."`           | `ACTIDINL`    |
| `UPDATE-ACCTDAT-FILE` (REWRITE) | `NOTFND`             | R30   | `"Account ID NOT found..."`               | `ACTIDINL`    |
| `UPDATE-ACCTDAT-FILE` (REWRITE) | any other non-normal | R31   | `"Unable to Update Account..."`           | `ACTIDINL`    |
| `READ-CXACAIX-FILE`             | `NOTFND`             | R32   | `"Account ID NOT found..."`               | `ACTIDINL`    |
| `READ-CXACAIX-FILE`             | any other non-normal | R33   | `"Unable to lookup XREF AIX file..."`     | `ACTIDINL`    |
| `STARTBR-TRANSACT-FILE`         | `NOTFND`             | R34   | `"Transaction ID NOT found..."`           | `ACTIDINL`    |
| `STARTBR-TRANSACT-FILE`         | any other non-normal | R35   | `"Unable to lookup Transaction..."`       | `ACTIDINL`    |
| `READPREV-TRANSACT-FILE`        | `ENDFILE`            | R36   | *(no error — seeds `TRAN-ID := 0`)*       | —             |
| `READPREV-TRANSACT-FILE`        | any other non-normal | R37   | `"Unable to lookup Transaction..."`       | `ACTIDINL`    |
| `WRITE-TRANSACT-FILE`           | `DUPKEY` / `DUPREC`  | R38   | `"Tran ID already exist..."`              | `ACTIDINL`    |
| `WRITE-TRANSACT-FILE`           | any other non-normal | R39   | `"Unable to Add Bill pay Transaction..."` | `ACTIDINL`    |

All error paths:

* **R40.** Set `WS-ERR-FLG := 'Y'` so downstream steps in
  `PROCESS-ENTER-KEY` are bypassed.
* **R41.** Invoke `SEND-BILLPAY-SCREEN` to re-render the map with the error
  message.

### S11 — Timestamp formatting

*Used by:* R23 (ORIG-TS, PROC-TS).

* **R42.** Capture current time via `EXEC CICS ASKTIME ABSTIME(...)` then
  format to `YYYY-MM-DD HH:MM:SS.000000` (26 characters, per `TRAN-ORIG-TS`
  layout `PIC X(26)`) using `FORMATTIME` with `DATESEP('-')` and
  `TIMESEP(':')`. Milliseconds position is zeroed.

### S12 — Header / footer population

*Invoked by:* `SEND-BILLPAY-SCREEN` → `POPULATE-HEADER-INFO`.

* **R43.** On every outbound send, populate title fields (`CCDA-TITLE01`,
  `CCDA-TITLE02`), program name, transaction name, current date
  (`MM/DD/YY`), current time (`HH:MM:SS`), and place `WS-MESSAGE` into
  `ERRMSGO`.

---

## 3. Invariants

* **I1.** The paid amount equals the account balance at the moment of the
  READ; after REWRITE the new balance is `0.00`. (COBIL00C does not support
  partial payments.)
* **I2.** A successful run produces exactly one new `TRAN-RECORD` and exactly
  one updated `ACCOUNT-RECORD` — both or neither, enforced in COBOL by the
  pseudo-conversational turn committing atomically at `EXEC CICS RETURN`.
* **I3.** The generated transaction ID is strictly `max(existing TRAN-ID) +
  1`, or `1` when the file is empty.
* **I4.** The merchant is a synthetic "self" merchant: ID `999999999`, name
  `"BILL PAYMENT"`, city/zip `"N/A"`.

---

## 4. Traceability table

Each rule R# maps to a COBOL paragraph / line range in `app/cbl/COBIL00C.cbl`.

| Rule | Paragraph / construct                    | Lines       |
|------|------------------------------------------|-------------|
| R1   | `MAIN-PARA` `IF EIBCALEN = 0` branch     | 107–109     |
| R2   | `MAIN-PARA` — `SET CDEMO-PGM-REENTER`    | 112–113     |
| R3   | `MAIN-PARA` — init map, cursor           | 114–115     |
| R4   | `MAIN-PARA` — pre-selected tran branch   | 116–121     |
| R5   | `MAIN-PARA` — `PERFORM SEND-BILLPAY-SCREEN` | 122      |
| R6   | `EVALUATE EIBAID` `WHEN DFHENTER`        | 126–127     |
| R7   | `EVALUATE EIBAID` `WHEN DFHPF3`          | 128–135     |
| R8   | `EVALUATE EIBAID` `WHEN DFHPF4`          | 136–137     |
| R9   | `EVALUATE EIBAID` `WHEN OTHER`           | 138–141     |
| R10  | `EXEC CICS RETURN TRANSID COMMAREA`      | 146–149     |
| R11  | `PROCESS-ENTER-KEY` blank ACCT-ID guard  | 158–167     |
| R12  | `PROCESS-ENTER-KEY` CONFIRM invalid branch | 185–190   |
| R13  | `PROCESS-ENTER-KEY` MOVE to ACCT-ID + XREF-ACCT-ID | 170–171 |
| R14  | `PROCESS-ENTER-KEY` → `READ-ACCTDAT-FILE` | 184 / 343–372 |
| R15  | `MOVE ACCT-CURR-BAL → WS-CURR-BAL → CURBALI` | 193–194 |
| R16  | `IF ACCT-CURR-BAL <= ZEROS` branch       | 197–206     |
| R17  | `IF CONF-PAY-YES ... ELSE ... END-IF`    | 210, 236–240 |
| R18  | `WHEN 'N' / 'n'` → `CLEAR-CURRENT-SCREEN` | 178–181    |
| R19  | `MOVE 'Y' TO WS-ERR-FLG`                 | 181         |
| R20  | `READ-ACCTDAT-FILE` with `UPDATE`        | 345–354     |
| R21  | `READ-CXACAIX-FILE` (`PERFORM`)          | 211 / 408–436 |
| R22  | `STARTBR` + `READPREV` + `ENDBR`         | 212–217 / 441–505 |
| R23  | MOVE statements for TRAN-RECORD fields   | 218–229     |
| R24  | `WRITE-TRANSACT-FILE`                    | 233 / 510–520 |
| R25  | `COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT` | 234 |
| R26  | `UPDATE-ACCTDAT-FILE` (REWRITE)          | 235 / 377–385 |
| R27  | `WRITE-TRANSACT-FILE` success branch     | 523–532     |
| R28  | `READ-ACCTDAT-FILE` NOTFND               | 359–364     |
| R29  | `READ-ACCTDAT-FILE` WHEN OTHER           | 365–371     |
| R30  | `UPDATE-ACCTDAT-FILE` NOTFND             | 390–395     |
| R31  | `UPDATE-ACCTDAT-FILE` WHEN OTHER         | 396–402     |
| R32  | `READ-CXACAIX-FILE` NOTFND               | 423–428     |
| R33  | `READ-CXACAIX-FILE` WHEN OTHER           | 429–435     |
| R34  | `STARTBR-TRANSACT-FILE` NOTFND           | 454–459     |
| R35  | `STARTBR-TRANSACT-FILE` WHEN OTHER       | 460–466     |
| R36  | `READPREV-TRANSACT-FILE` ENDFILE         | 487–488     |
| R37  | `READPREV-TRANSACT-FILE` WHEN OTHER      | 489–495     |
| R38  | `WRITE-TRANSACT-FILE` DUPKEY / DUPREC    | 533–539     |
| R39  | `WRITE-TRANSACT-FILE` WHEN OTHER         | 540–546     |
| R40  | `MOVE 'Y' TO WS-ERR-FLG` (all error sites) | 160, 186, 200, 360, 367, 391, 398, 424, 431, 455, 462, 491, 535, 542 |
| R41  | `PERFORM SEND-BILLPAY-SCREEN` (all error sites) | 164, 190, 204, 364, 371, 395, 402, 428, 435, 459, 466, 495, 539, 546 |
| R42  | `GET-CURRENT-TIMESTAMP`                  | 249–267     |
| R43  | `POPULATE-HEADER-INFO`                   | 319–338     |

---

## 5. Java mapping (informational)

| COBOL artefact          | Java 21 / Spring Boot 3 artefact                             |
|-------------------------|--------------------------------------------------------------|
| `MAIN-PARA` AID routing | `BillPaymentController` endpoints (`/api/billpay`, `/clear`) |
| `PROCESS-ENTER-KEY`     | `BillPaymentService.process(BillPaymentRequest)`             |
| `ACCOUNT-RECORD`        | `Account` record + `AccountRepository`                       |
| `CARD-XREF-RECORD`      | `CardXref` record + `CardXrefRepository`                     |
| `TRAN-RECORD`           | `Transaction` record + `TransactionRepository`               |
| `WS-MESSAGE` + `WS-ERR-FLG` | `BillPaymentResponse.message` + `status` enum            |
| `DFHRESP(NOTFND)` etc.  | `AccountNotFoundException`, `XrefNotFoundException`, `DuplicateTransactionException`, `VsamIoException` |
| Timestamp (R42)         | `Clock.systemDefaultZone()` formatted as `yyyy-MM-dd HH:mm:ss.SSSSSS` |

See [`java/carddemo-billpay/`](../java/carddemo-billpay/) for the full
implementation and tests.
