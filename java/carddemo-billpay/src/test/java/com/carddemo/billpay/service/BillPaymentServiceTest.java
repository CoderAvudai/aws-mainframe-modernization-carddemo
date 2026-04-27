package com.carddemo.billpay.service;

import com.carddemo.billpay.domain.Account;
import com.carddemo.billpay.domain.CardXref;
import com.carddemo.billpay.domain.Transaction;
import com.carddemo.billpay.exception.AccountNotFoundException;
import com.carddemo.billpay.exception.DuplicateTransactionException;
import com.carddemo.billpay.exception.ValidationException;
import com.carddemo.billpay.repo.InMemoryAccountRepository;
import com.carddemo.billpay.repo.InMemoryCardXrefRepository;
import com.carddemo.billpay.repo.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scenario coverage matches {@code docs/COBIL00C_SPEC.md}.  Each nested class
 * corresponds to one scenario {@code S#} in the spec and the individual test
 * names cite the rule identifiers being verified.
 */
class BillPaymentServiceTest {

    private static final String ACCT_ID = "00000000010";
    private static final String OTHER_ACCT = "00000000020";
    private static final String CARD_NUM = "4111111111111111";

    private InMemoryAccountRepository accountRepo;
    private InMemoryCardXrefRepository xrefRepo;
    private InMemoryTransactionRepository tranRepo;
    private BillPaymentService service;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryAccountRepository();
        xrefRepo = new InMemoryCardXrefRepository();
        tranRepo = new InMemoryTransactionRepository();
        Clock fixed = Clock.fixed(Instant.parse("2025-01-02T03:04:05.678901Z"), ZoneOffset.UTC);
        service = new BillPaymentService(accountRepo, xrefRepo, tranRepo, fixed);
    }

    private Account account(BigDecimal balance) {
        return new Account(
                ACCT_ID,
                "Y",
                balance,
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                "2022-01-01",
                "2027-01-01",
                "2027-01-01",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "12345",
                "GRP01");
    }

    private void seedHappyPath(BigDecimal balance) {
        accountRepo.seed(account(balance));
        xrefRepo.seed(new CardXref(CARD_NUM, "000000001", ACCT_ID));
    }

    // ---------------------------------------------------------------
    // S5 — Validation: account ID (R11)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("S5 — Account ID validation (R11)")
    class AccountIdValidation {

        @Test
        @DisplayName("R11 rejects null account id")
        void rejectsNullAccountId() {
            assertThatThrownBy(() -> service.process(new BillPaymentRequest(null, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage(ValidationException.EMPTY_ACCT_ID);
        }

        @Test
        @DisplayName("R11 rejects blank account id")
        void rejectsBlankAccountId() {
            assertThatThrownBy(() -> service.process(new BillPaymentRequest("   ", null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage(ValidationException.EMPTY_ACCT_ID);
        }
    }

    // ---------------------------------------------------------------
    // S6 — Validation: CONFIRM value (R12)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("S6 — CONFIRM validation (R12)")
    class ConfirmValidation {

        @Test
        @DisplayName("R12 rejects CONFIRM='X'")
        void rejectsX() {
            assertThatThrownBy(() -> service.process(new BillPaymentRequest(ACCT_ID, "X")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage(ValidationException.INVALID_CONFIRM);
        }

        @Test
        @DisplayName("R12 rejects multi-character CONFIRM")
        void rejectsMultiChar() {
            assertThatThrownBy(() -> service.process(new BillPaymentRequest(ACCT_ID, "YES")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage(ValidationException.INVALID_CONFIRM);
        }
    }

    // ---------------------------------------------------------------
    // S7 — Inquiry path (R13–R17)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("S7 — Inquiry path (R13–R17)")
    class InquiryPath {

        @Test
        @DisplayName("R17 returns balance and prompt on blank CONFIRM")
        void returnsBalanceAndPrompt() {
            seedHappyPath(new BigDecimal("250.75"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, null));
            assertThat(resp.status()).isEqualTo(BillPaymentStatus.AWAITING_CONFIRMATION);
            assertThat(resp.currentBalance()).isEqualByComparingTo("250.75");
            assertThat(resp.message()).isEqualTo("Confirm to make a bill payment...");
            assertThat(resp.transactionId()).isNull();
            assertThat(tranRepo.size()).isZero();
        }

        @Test
        @DisplayName("R17 treats empty string the same as blank / LOW-VALUES")
        void emptyStringSameAsBlank() {
            seedHappyPath(new BigDecimal("1.00"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, ""));
            assertThat(resp.status()).isEqualTo(BillPaymentStatus.AWAITING_CONFIRMATION);
        }

        @Test
        @DisplayName("R16 reports 'nothing to pay' when balance is zero")
        void nothingToPayZero() {
            seedHappyPath(BigDecimal.ZERO);
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, null));
            assertThat(resp.status()).isEqualTo(BillPaymentStatus.NOTHING_TO_PAY);
            assertThat(resp.message()).isEqualTo("You have nothing to pay...");
        }

        @Test
        @DisplayName("R16 reports 'nothing to pay' when balance is negative")
        void nothingToPayNegative() {
            seedHappyPath(new BigDecimal("-10.00"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, "Y"));
            assertThat(resp.status()).isEqualTo(BillPaymentStatus.NOTHING_TO_PAY);
            assertThat(resp.message()).isEqualTo("You have nothing to pay...");
        }
    }

    // ---------------------------------------------------------------
    // S8 — Cancellation (R18 / R19)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("S8 — Cancellation (R18 / R19)")
    class Cancellation {

        @Test
        @DisplayName("R18 CONFIRM='N' does not touch any dataset")
        void cancellationUpperN() {
            seedHappyPath(new BigDecimal("100.00"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, "N"));
            assertThat(resp.status()).isEqualTo(BillPaymentStatus.CANCELLED);
            assertThat(tranRepo.size()).isZero();
            // Account balance untouched
            assertThat(accountRepo.findById(ACCT_ID).orElseThrow().currentBalance())
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("R18 accepts lower-case 'n'")
        void cancellationLowerN() {
            seedHappyPath(new BigDecimal("100.00"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, "n"));
            assertThat(resp.status()).isEqualTo(BillPaymentStatus.CANCELLED);
        }
    }

    // ---------------------------------------------------------------
    // S9 — Successful bill payment (R20–R27)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("S9 — Successful payment (R20–R27)")
    class HappyPath {

        @Test
        @DisplayName("R22–R27 pays full balance, writes transaction, rewrites account")
        void fullBalancePayment() {
            seedHappyPath(new BigDecimal("500.00"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, "Y"));

            assertThat(resp.status()).isEqualTo(BillPaymentStatus.PAYMENT_SUCCESSFUL);
            assertThat(resp.transactionId()).isEqualTo("0000000000000001");
            assertThat(resp.message())
                    .isEqualTo("Payment successful. Your Transaction ID is 0000000000000001.");

            // R25 — balance reduced to zero
            Account after = accountRepo.findById(ACCT_ID).orElseThrow();
            assertThat(after.currentBalance()).isEqualByComparingTo(BigDecimal.ZERO);

            // R23 — fixed-field content of the TRAN-RECORD
            Transaction written = tranRepo.findHighestTransactionId().orElseThrow();
            assertThat(written.typeCode()).isEqualTo(Transaction.TYPE_CODE_BILL_PAYMENT);
            assertThat(written.categoryCode()).isEqualTo(Transaction.CATEGORY_BILL_PAYMENT);
            assertThat(written.source()).isEqualTo("POS TERM");
            assertThat(written.description()).isEqualTo("BILL PAYMENT - ONLINE");
            assertThat(written.amount()).isEqualByComparingTo("500.00");
            assertThat(written.merchantId()).isEqualTo(Transaction.MERCHANT_ID_BILL_PAYMENT);
            assertThat(written.merchantName()).isEqualTo("BILL PAYMENT");
            assertThat(written.merchantCity()).isEqualTo("N/A");
            assertThat(written.merchantZip()).isEqualTo("N/A");
            assertThat(written.cardNumber()).isEqualTo(CARD_NUM);

            // R42 — timestamp formatted to 26-character COBOL layout
            assertThat(written.originationTimestamp()).hasSize(26);
            assertThat(written.processingTimestamp())
                    .isEqualTo(written.originationTimestamp());
        }

        @Test
        @DisplayName("R22 increments TRAN-ID when transactions already exist")
        void incrementsTranId() {
            seedHappyPath(new BigDecimal("50.00"));
            tranRepo.save(new Transaction(
                    "0000000000000041", "01", 1, "POS", "prior", BigDecimal.ONE,
                    1L, "M", "C", "Z", CARD_NUM, "ts", "ts"));

            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, "y"));
            assertThat(resp.transactionId()).isEqualTo("0000000000000042");
        }
    }

    // ---------------------------------------------------------------
    // S10 — File error paths (R28 / R32)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("S10 — File error paths (R28 / R32)")
    class FileErrorPaths {

        @Test
        @DisplayName("R28 ACCTDAT NOTFND surfaces 'Account ID NOT found...'")
        void accountNotFound() {
            assertThatThrownBy(() -> service.process(new BillPaymentRequest(OTHER_ACCT, null)))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage(AccountNotFoundException.MESSAGE);
        }

        @Test
        @DisplayName("R32 CXACAIX NOTFND surfaces 'Account ID NOT found...'")
        void xrefNotFound() {
            // Seed account but not xref
            accountRepo.seed(account(new BigDecimal("10.00")));
            assertThatThrownBy(() -> service.process(new BillPaymentRequest(ACCT_ID, "Y")))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage(AccountNotFoundException.MESSAGE);
        }

        @Test
        @DisplayName("R38 duplicate TRAN-ID surfaces 'Tran ID already exist...'")
        void duplicateTransaction() {
            // Use a custom repository that reports a stale "highest" id so the
            // computed next-id already exists in the store — faithfully models
            // the COBOL DUPKEY / DUPREC race (R38).
            InMemoryTransactionRepository duplicating = new InMemoryTransactionRepository() {
                @Override
                public java.util.Optional<Transaction> findHighestTransactionId() {
                    return java.util.Optional.of(new Transaction(
                            "0000000000000000", "01", 1, "POS", "prior",
                            BigDecimal.ONE, 1L, "M", "C", "Z", CARD_NUM,
                            "ts", "ts"));
                }

                @Override
                public boolean existsById(String transactionId) {
                    return "0000000000000001".equals(transactionId);
                }
            };
            BillPaymentService dupService = new BillPaymentService(
                    accountRepo, xrefRepo, duplicating,
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
            seedHappyPath(new BigDecimal("25.00"));

            assertThatThrownBy(() -> dupService.process(new BillPaymentRequest(ACCT_ID, "Y")))
                    .isInstanceOf(DuplicateTransactionException.class)
                    .hasMessage(DuplicateTransactionException.MESSAGE);
        }
    }

    // ---------------------------------------------------------------
    // Invariant coverage (I1–I4)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Invariants I1–I4")
    class Invariants {

        @Test
        @DisplayName("I1 paid amount equals starting balance; final balance is zero")
        void paidAmountEqualsStartingBalance() {
            seedHappyPath(new BigDecimal("1234.56"));
            service.process(new BillPaymentRequest(ACCT_ID, "Y"));
            Transaction t = tranRepo.findHighestTransactionId().orElseThrow();
            assertThat(t.amount()).isEqualByComparingTo("1234.56");
            assertThat(accountRepo.findById(ACCT_ID).orElseThrow().currentBalance())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("I2 no partial state on xref failure")
        void noPartialStateOnXrefFailure() {
            accountRepo.seed(account(new BigDecimal("77.00")));
            assertThatThrownBy(() -> service.process(new BillPaymentRequest(ACCT_ID, "Y")))
                    .isInstanceOf(AccountNotFoundException.class);
            // Balance unchanged
            assertThat(accountRepo.findById(ACCT_ID).orElseThrow().currentBalance())
                    .isEqualByComparingTo("77.00");
            // No transaction written
            assertThat(tranRepo.size()).isZero();
        }

        @Test
        @DisplayName("I3 first transaction gets id 1")
        void firstTransactionIdIsOne() {
            seedHappyPath(new BigDecimal("10.00"));
            BillPaymentResponse resp = service.process(new BillPaymentRequest(ACCT_ID, "Y"));
            assertThat(resp.transactionId()).isEqualTo("0000000000000001");
        }

        @Test
        @DisplayName("I4 merchant is the synthetic self-merchant")
        void syntheticMerchant() {
            seedHappyPath(new BigDecimal("10.00"));
            service.process(new BillPaymentRequest(ACCT_ID, "Y"));
            Transaction t = tranRepo.findHighestTransactionId().orElseThrow();
            assertThat(t.merchantId()).isEqualTo(999_999_999L);
            assertThat(t.merchantName()).isEqualTo("BILL PAYMENT");
            assertThat(t.merchantCity()).isEqualTo("N/A");
            assertThat(t.merchantZip()).isEqualTo("N/A");
        }
    }
}
