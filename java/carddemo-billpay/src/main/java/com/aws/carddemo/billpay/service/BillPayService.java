package com.aws.carddemo.billpay.service;

import com.aws.carddemo.billpay.exception.AccountNotFoundException;
import com.aws.carddemo.billpay.exception.CardXrefNotFoundException;
import com.aws.carddemo.billpay.exception.DuplicateTransactionException;
import com.aws.carddemo.billpay.exception.InvalidConfirmationException;
import com.aws.carddemo.billpay.exception.NothingToPayException;
import com.aws.carddemo.billpay.model.Account;
import com.aws.carddemo.billpay.model.BalanceInquiryResponse;
import com.aws.carddemo.billpay.model.BillPayResponse;
import com.aws.carddemo.billpay.model.CardXref;
import com.aws.carddemo.billpay.model.Transaction;
import com.aws.carddemo.billpay.repository.AccountRepository;
import com.aws.carddemo.billpay.repository.CardXrefRepository;
import com.aws.carddemo.billpay.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implements every business rule from COBIL00C (bill payment).
 * See docs/COBIL00C-business-rules.md for the full specification.
 */
@Service
public class BillPayService {

    private static final String TRAN_TYPE_CD = "02";
    private static final int TRAN_CAT_CD = 2;
    private static final String TRAN_SOURCE = "POS TERM";
    private static final String TRAN_DESC = "BILL PAYMENT - ONLINE";
    private static final long MERCHANT_ID = 999999999L;
    private static final String MERCHANT_NAME = "BILL PAYMENT";
    private static final String MERCHANT_CITY = "N/A";
    private static final String MERCHANT_ZIP = "N/A";

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;

    public BillPayService(AccountRepository accountRepository,
                          CardXrefRepository cardXrefRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRepository = transactionRepository;
    }

    // --- Rule 4.4 / 5.1: look up account, check balance, return inquiry ---
    public BalanceInquiryResponse lookupBalance(String accountId) {
        Account account = findAccountOrThrow(accountId);
        validatePositiveBalance(account);
        return new BalanceInquiryResponse(
                account.getAcctId(),
                account.getCurrentBalance(),
                "Confirm to make a bill payment"
        );
    }

    // --- Main payment entry point (§3-§10) ---
    @Transactional
    public BillPayResponse processPayment(String accountId, String confirm) {
        validateConfirmation(confirm);

        if ("N".equalsIgnoreCase(confirm)) {
            // Rule 4.3: user declined – cancel without account lookup
            return new BillPayResponse(
                    "Payment cancelled",
                    null,
                    null,
                    null
            );
        }

        Account account = findAccountOrThrow(accountId);
        validatePositiveBalance(account);

        if (isBlankOrNull(confirm)) {
            // Rule 4.4: first submission – return balance for confirmation
            return new BillPayResponse(
                    "Confirm to make a bill payment",
                    null,
                    account.getCurrentBalance(),
                    account.getCurrentBalance()
            );
        }

        // Rule 4.5: confirmed (Y/y) – execute payment
        return executePayment(account);
    }

    // --- Rule 4.2: validate confirmation field ---
    void validateConfirmation(String confirm) {
        if (isBlankOrNull(confirm)) {
            return;
        }
        if (!"Y".equalsIgnoreCase(confirm) && !"N".equalsIgnoreCase(confirm)) {
            throw new InvalidConfirmationException();
        }
    }

    // --- Rule 3.2-3.4: look up account ---
    Account findAccountOrThrow(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    // --- Rule 5.1: balance must be positive ---
    void validatePositiveBalance(Account account) {
        if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NothingToPayException();
        }
    }

    // --- Rules 6-10: card lookup, ID generation, write txn, update acct ---
    BillPayResponse executePayment(Account account) {
        // Rule 6.1: look up card cross-reference
        CardXref xref = cardXrefRepository.findByAcctId(account.getAcctId())
                .orElseThrow(() -> new CardXrefNotFoundException(account.getAcctId()));

        // Rule 7: generate next transaction ID
        String nextTranId = generateNextTransactionId();

        // Rule 8: build transaction record
        BigDecimal paymentAmount = account.getCurrentBalance();
        String timestamp = formatTimestamp(LocalDateTime.now());

        Transaction txn = new Transaction();
        txn.setTranId(nextTranId);
        txn.setTranTypeCd(TRAN_TYPE_CD);
        txn.setTranCatCd(TRAN_CAT_CD);
        txn.setTranSource(TRAN_SOURCE);
        txn.setTranDesc(TRAN_DESC);
        txn.setTranAmt(paymentAmount);
        txn.setCardNum(xref.getCardNum());
        txn.setMerchantId(MERCHANT_ID);
        txn.setMerchantName(MERCHANT_NAME);
        txn.setMerchantCity(MERCHANT_CITY);
        txn.setMerchantZip(MERCHANT_ZIP);
        txn.setOrigTs(timestamp);
        txn.setProcTs(timestamp);

        // Rule 10: write transaction
        writeTransaction(txn);

        // Rule 9.1: new_balance = current_balance - transaction_amount
        BigDecimal newBalance = account.getCurrentBalance().subtract(paymentAmount);
        account.setCurrentBalance(newBalance);

        // Rule 9.2: persist updated account
        accountRepository.save(account);

        // Rule 10.1: success message
        return new BillPayResponse(
                "Payment successful. Your Transaction ID is " + nextTranId + ".",
                nextTranId,
                paymentAmount,
                newBalance
        );
    }

    // --- Rule 7.1-7.4: find highest ID, add 1 ---
    String generateNextTransactionId() {
        return transactionRepository.findTopByOrderByTranIdDesc()
                .map(last -> {
                    long lastId = Long.parseLong(last.getTranId().trim());
                    return formatTranId(lastId + 1);
                })
                .orElse(formatTranId(1));
    }

    // --- Rule 10.2-10.3: handle write errors ---
    private void writeTransaction(Transaction txn) {
        if (transactionRepository.existsById(txn.getTranId())) {
            throw new DuplicateTransactionException(txn.getTranId());
        }
        try {
            transactionRepository.save(txn);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateTransactionException(txn.getTranId());
        }
    }

    String formatTimestamp(LocalDateTime dateTime) {
        return dateTime.format(TS_FORMAT);
    }

    private String formatTranId(long id) {
        return String.format("%016d", id);
    }

    private boolean isBlankOrNull(String value) {
        return value == null || value.isBlank();
    }
}
