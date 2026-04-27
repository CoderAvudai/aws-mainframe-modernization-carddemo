# COBIL00C – Bill Payment Business Rules Specification

> **Source program**: `app/cbl/COBIL00C.cbl` (CICS COBOL, transaction **CB00**)
>
> **Purpose**: Allows an authenticated user to pay their credit-card account
> balance **in full** via a single online bill-payment transaction.

---

## 1  Session & Authentication Guard

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 1.1 | If no communication area is present (`EIBCALEN = 0`), the user is not authenticated. Redirect to the sign-on program (`COSGN00C`). | lines 107-109 |

## 2  Screen Navigation Keys

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 2.1 | **Enter** – submit the bill-payment form (see §3–§8). | line 127 |
| 2.2 | **PF3** – return to the previous screen. If no previous screen is recorded, return to the main menu (`COMEN01C`). | lines 128-135 |
| 2.3 | **PF4** – clear all input fields and reset the screen. | lines 136-137 |
| 2.4 | **Any other key** – display error *"Invalid key pressed. Please see below…"* | lines 138-141 |

## 3  Account ID Validation

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 3.1 | The Account ID field must not be empty (spaces or low-values). Error: *"Acct ID can NOT be empty…"* | lines 159-164 |
| 3.2 | The Account ID is used to look up the account record in the Account Master file (`ACCTDAT`). | lines 345-354 |
| 3.3 | If the account is not found, display error *"Account ID NOT found…"* | lines 359-364 |
| 3.4 | Any other file-read error produces *"Unable to lookup Account…"* | lines 365-371 |

## 4  Confirmation Field Validation

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 4.1 | Accepted values: `Y`, `y`, `N`, `n`, blank (spaces/low-values). | lines 173-184 |
| 4.2 | Any other value → error *"Invalid value. Valid values are (Y/N)…"* | lines 185-190 |
| 4.3 | `N` or `n` → cancel the payment and clear the screen. | lines 178-181 |
| 4.4 | Blank (first submission) → look up the account, display the current balance, and prompt the user to confirm with *"Confirm to make a bill payment…"* | lines 182-184, 237-239 |
| 4.5 | `Y` or `y` → proceed with the confirmed payment flow (§5–§8). | lines 174-177 |

## 5  Balance Check

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 5.1 | After the account is retrieved, if the current balance is ≤ 0, reject the payment with *"You have nothing to pay…"* | lines 198-205 |

## 6  Card Cross-Reference Lookup

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 6.1 | Look up the card cross-reference record (`CXACAIX` file) by account ID to obtain the associated card number. | lines 408-418 |
| 6.2 | Not found → *"Account ID NOT found…"* | lines 423-428 |
| 6.3 | Other error → *"Unable to lookup XREF AIX file…"* | lines 429-435 |

## 7  Transaction ID Generation

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 7.1 | Position a browse cursor at the **end** of the Transaction file (`TRANSACT`) using `HIGH-VALUES`. | lines 212-213, 441-467 |
| 7.2 | Read the **previous** (i.e. last) record to obtain the highest existing transaction ID. | lines 214, 472-496 |
| 7.3 | If the file is empty (end-of-file on read-previous), treat the last ID as **0**. | lines 487-488 |
| 7.4 | The new transaction ID = last transaction ID + 1. | lines 216-217 |
| 7.5 | Close the browse cursor after reading. | lines 215, 501-505 |

## 8  Transaction Record Construction & Persistence

| # | Field | Value | COBOL Reference |
|---|-------|-------|-----------------|
| 8.1 | Transaction ID | Auto-generated (§7) | line 219 |
| 8.2 | Type Code | `"02"` | line 220 |
| 8.3 | Category Code | `2` | line 221 |
| 8.4 | Source | `"POS TERM"` | line 222 |
| 8.5 | Description | `"BILL PAYMENT - ONLINE"` | line 223 |
| 8.6 | Amount | The account's **current balance** at time of payment | line 224 |
| 8.7 | Card Number | From cross-reference record (§6) | line 225 |
| 8.8 | Merchant ID | `999999999` | line 226 |
| 8.9 | Merchant Name | `"BILL PAYMENT"` | line 227 |
| 8.10 | Merchant City | `"N/A"` | line 228 |
| 8.11 | Merchant ZIP | `"N/A"` | line 229 |
| 8.12 | Origination Timestamp | Current date/time as `YYYY-MM-DD HH:MM:SS.000000` | lines 230-231 |
| 8.13 | Processing Timestamp | Same as origination timestamp | line 232 |

## 9  Account Balance Update

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 9.1 | After the transaction is written, reduce the account balance by the transaction amount: `new_balance = current_balance − transaction_amount`. Because the amount equals the full balance, this effectively zeros the balance. | line 234 |
| 9.2 | Persist the updated account record back to the Account Master file. | line 235 |
| 9.3 | Not-found on update → *"Account ID NOT found…"* | lines 390-395 |
| 9.4 | Other update error → *"Unable to Update Account…"* | lines 396-402 |

## 10  Transaction Write Outcomes

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 10.1 | **Success** – clear all fields, display (in green): *"Payment successful. Your Transaction ID is {id}."* | lines 523-532 |
| 10.2 | **Duplicate key** – error *"Tran ID already exist…"* | lines 533-539 |
| 10.3 | **Other write error** – error *"Unable to Add Bill pay Transaction…"* | lines 540-546 |

## 11  Screen Reset (Clear / Initialize)

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 11.1 | Clearing the screen resets Account ID, Current Balance, and Confirmation fields to spaces and removes any messages. | lines 560-566 |

## 12  Pre-selected Transaction Flow

| # | Rule | COBOL Reference |
|---|------|-----------------|
| 12.1 | If the program is entered for the first time **and** a transaction was pre-selected from a prior screen (`CDEMO-CB00-TRN-SELECTED` is not blank), pre-populate the Account ID field and immediately process it as if Enter was pressed. | lines 116-121 |
