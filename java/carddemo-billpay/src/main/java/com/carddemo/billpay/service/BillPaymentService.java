package com.carddemo.billpay.service;

import com.carddemo.billpay.domain.Account;
import com.carddemo.billpay.domain.CardXref;
import com.carddemo.billpay.domain.Transaction;
import com.carddemo.billpay.exception.AccountNotFoundException;
import com.carddemo.billpay.exception.DuplicateTransactionException;
import com.carddemo.billpay.exception.ValidationException;
import com.carddemo.billpay.repo.AccountRepository;
import com.carddemo.billpay.repo.CardXrefRepository;
import com.carddemo.billpay.repo.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Java 21 port of the COBIL00C {@code PROCESS-ENTER-KEY} paragraph and its
 * downstream I/O paragraphs. Every rule listed in {@code docs/COBIL00C_SPEC.md}
 * is annotated in-line so reviewers can trace Java behaviour back to the COBOL
 * source.
 */
@Service
public class BillPaymentService {

    /** Width of the {@code TRAN-ID} in {@code CVTRA05Y.cpy}. */
    private static final int TRAN_ID_WIDTH = 16;

    /** Width of {@code WS-TIMESTAMP} in {@code TRAN-ORIG-TS} / {@code TRAN-PROC-TS}. */
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.ROOT);

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public BillPaymentService(
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository,
            TransactionRepository transactionRepository,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    /**
     * Executes a single pseudo-conversational turn — the Java analogue of
     * COBIL00C's {@code PROCESS-ENTER-KEY}.
     */
    public BillPaymentResponse process(BillPaymentRequest request) {
        String accountId = requireAccountId(request);      // R11
        ConfirmChoice confirm = parseConfirm(request.confirm()); // R7 / R12

        if (confirm == ConfirmChoice.NO) {                  // R18 / R19
            return new BillPaymentResponse(
                    BillPaymentStatus.CANCELLED,
                    accountId,
                    null,
                    null,
                    null);
        }

        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(AccountNotFoundException::new); // R14 / R28

        BigDecimal balance = account.currentBalance();
        if (balance == null || balance.signum() <= 0) {     // R16
            return new BillPaymentResponse(
                    BillPaymentStatus.NOTHING_TO_PAY,
                    accountId,
                    balance,
                    null,
                    "You have nothing to pay...");
        }

        if (confirm == ConfirmChoice.BLANK) {               // R17
            return new BillPaymentResponse(
                    BillPaymentStatus.AWAITING_CONFIRMATION,
                    accountId,
                    balance,
                    null,
                    "Confirm to make a bill payment...");
        }

        // confirm == YES — execute the payment (S9).
        CardXref xref = cardXrefRepository
                .findByAccountId(accountId)
                .orElseThrow(AccountNotFoundException::new); // R21 / R32

        String nextTranId = generateNextTransactionId();    // R22

        Transaction tran = buildTransaction(nextTranId, balance, xref);  // R23
        if (transactionRepository.existsById(nextTranId)) { // R38
            throw new DuplicateTransactionException();
        }
        transactionRepository.save(tran);                   // R24

        Account updated = account.withCurrentBalance(
                balance.subtract(tran.amount()));           // R25
        accountRepository.update(updated);                  // R26

        return new BillPaymentResponse(                     // R27
                BillPaymentStatus.PAYMENT_SUCCESSFUL,
                accountId,
                updated.currentBalance(),
                nextTranId,
                "Payment successful. Your Transaction ID is " + nextTranId + ".");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** R11 — ACCT-ID must not be blank. */
    private static String requireAccountId(BillPaymentRequest request) {
        if (request == null
                || request.accountId() == null
                || request.accountId().isBlank()) {
            throw new ValidationException(ValidationException.EMPTY_ACCT_ID);
        }
        return request.accountId().trim();
    }

    /** R7 / R12 — parse the CONFIRM value. */
    private static ConfirmChoice parseConfirm(String raw) {
        if (raw == null || raw.isBlank()) {
            return ConfirmChoice.BLANK;
        }
        String trimmed = raw.trim();
        if (trimmed.length() != 1) {
            throw new ValidationException(ValidationException.INVALID_CONFIRM);
        }
        return switch (trimmed.charAt(0)) {
            case 'Y', 'y' -> ConfirmChoice.YES;
            case 'N', 'n' -> ConfirmChoice.NO;
            default -> throw new ValidationException(ValidationException.INVALID_CONFIRM);
        };
    }

    /** R22 — {@code STARTBR(HIGH-VALUES) + READPREV + ENDBR + (+1)}. */
    private String generateNextTransactionId() {
        long next = transactionRepository
                .findHighestTransactionId()
                .map(Transaction::transactionId)
                .map(BillPaymentService::parseTranId)
                .orElse(0L)
                + 1L;
        return formatTranId(next);
    }

    private static long parseTranId(String id) {
        if (id == null || id.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException nfe) {
            // The COBOL layout is PIC X(16) — if the file contains a
            // non-numeric id we treat it as empty and start from 1 (consistent
            // with R22's ENDFILE branch).
            return 0L;
        }
    }

    private static String formatTranId(long id) {
        String raw = Long.toString(id);
        if (raw.length() >= TRAN_ID_WIDTH) {
            return raw.substring(raw.length() - TRAN_ID_WIDTH);
        }
        return "0".repeat(TRAN_ID_WIDTH - raw.length()) + raw;
    }

    /** R23 — build the outgoing {@code TRAN-RECORD} with the fixed fields. */
    private Transaction buildTransaction(String tranId, BigDecimal amount, CardXref xref) {
        String now = LocalDateTime.now(clock).format(TIMESTAMP_FORMAT); // R42
        return new Transaction(
                tranId,
                Transaction.TYPE_CODE_BILL_PAYMENT,
                Transaction.CATEGORY_BILL_PAYMENT,
                "POS TERM",
                "BILL PAYMENT - ONLINE",
                amount,
                Transaction.MERCHANT_ID_BILL_PAYMENT,
                "BILL PAYMENT",
                "N/A",
                "N/A",
                xref.cardNumber(),
                now,
                now);
    }

    private enum ConfirmChoice {
        YES, NO, BLANK
    }
}
