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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillPayServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CardXrefRepository cardXrefRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private BillPayService service;

    private Account testAccount;
    private CardXref testXref;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId("00000000001");
        testAccount.setCurrentBalance(new BigDecimal("1500.00"));
        testAccount.setActiveStatus("Y");

        testXref = new CardXref();
        testXref.setCardNum("4111111111111111");
        testXref.setCustId("000000001");
        testXref.setAcctId("00000000001");
    }

    // ---------------------------------------------------------------
    // §3 Account ID Validation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 3: Account ID Validation")
    class AccountIdValidation {

        @Test
        @DisplayName("3.2-3.3: Account not found throws AccountNotFoundException")
        void accountNotFound() {
            when(accountRepository.findById("99999999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processPayment("99999999999", "Y"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Account ID NOT found");
        }

        @Test
        @DisplayName("3.2: Valid account ID succeeds lookup")
        void validAccountFound() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            BillPayResponse response = service.processPayment("00000000001", "Y");

            assertThat(response.message()).contains("Payment successful");
        }
    }

    // ---------------------------------------------------------------
    // §4 Confirmation Field Validation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 4: Confirmation Field Validation")
    class ConfirmationValidation {

        @ParameterizedTest
        @ValueSource(strings = {"X", "1", "YES", "NO", "maybe"})
        @DisplayName("4.2: Invalid confirmation value throws InvalidConfirmationException")
        void invalidConfirmation(String badValue) {
            assertThatThrownBy(() -> service.processPayment("00000000001", badValue))
                    .isInstanceOf(InvalidConfirmationException.class)
                    .hasMessageContaining("Invalid value. Valid values are (Y/N)");
        }

        @Test
        @DisplayName("4.3: Confirmation N cancels payment")
        void confirmNo() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            BillPayResponse resp = service.processPayment("00000000001", "N");

            assertThat(resp.message()).isEqualTo("Payment cancelled");
            assertThat(resp.transactionId()).isNull();
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("4.3: Confirmation n (lowercase) also cancels")
        void confirmNoLowercase() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            BillPayResponse resp = service.processPayment("00000000001", "n");

            assertThat(resp.message()).isEqualTo("Payment cancelled");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("4.4: Blank confirmation prompts for confirmation")
        void blankConfirmation(String blank) {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            BillPayResponse resp = service.processPayment("00000000001", blank);

            assertThat(resp.message()).isEqualTo("Confirm to make a bill payment");
            assertThat(resp.transactionId()).isNull();
            assertThat(resp.previousBalance()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("4.5: Confirmation Y proceeds to payment")
        void confirmYes() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            BillPayResponse resp = service.processPayment("00000000001", "Y");

            assertThat(resp.message()).contains("Payment successful");
            assertThat(resp.transactionId()).isNotNull();
        }

        @Test
        @DisplayName("4.5: Confirmation y (lowercase) also proceeds")
        void confirmYesLowercase() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            BillPayResponse resp = service.processPayment("00000000001", "y");

            assertThat(resp.message()).contains("Payment successful");
        }
    }

    // ---------------------------------------------------------------
    // §5 Balance Check
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 5: Balance Check")
    class BalanceCheck {

        @Test
        @DisplayName("5.1: Zero balance throws NothingToPayException")
        void zeroBalance() {
            testAccount.setCurrentBalance(BigDecimal.ZERO);
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> service.processPayment("00000000001", "Y"))
                    .isInstanceOf(NothingToPayException.class)
                    .hasMessageContaining("You have nothing to pay");
        }

        @Test
        @DisplayName("5.1: Negative balance throws NothingToPayException")
        void negativeBalance() {
            testAccount.setCurrentBalance(new BigDecimal("-100.00"));
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> service.processPayment("00000000001", "Y"))
                    .isInstanceOf(NothingToPayException.class);
        }

        @Test
        @DisplayName("5.1: Positive balance passes validation")
        void positiveBalance() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            BillPayResponse resp = service.processPayment("00000000001", "Y");

            assertThat(resp.message()).contains("Payment successful");
        }
    }

    // ---------------------------------------------------------------
    // §6 Card Cross-Reference Lookup
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 6: Card Cross-Reference Lookup")
    class CardXrefLookup {

        @Test
        @DisplayName("6.2: Missing card xref throws CardXrefNotFoundException")
        void xrefNotFound() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processPayment("00000000001", "Y"))
                    .isInstanceOf(CardXrefNotFoundException.class)
                    .hasMessageContaining("Card cross-reference NOT found");
        }

        @Test
        @DisplayName("6.1: Card number from xref is written to transaction")
        void xrefCardNumUsed() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            service.processPayment("00000000001", "Y");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getCardNum()).isEqualTo("4111111111111111");
        }
    }

    // ---------------------------------------------------------------
    // §7 Transaction ID Generation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 7: Transaction ID Generation")
    class TranIdGeneration {

        @Test
        @DisplayName("7.3-7.4: Empty file starts at 0000000000000001")
        void emptyFileStartsAtOne() {
            String nextId = service.generateNextTransactionId();
            assertThat(nextId).isEqualTo("0000000000000001");
        }

        @Test
        @DisplayName("7.2-7.4: Next ID is last + 1")
        void incrementsLastId() {
            Transaction last = new Transaction();
            last.setTranId("0000000000000042");
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.of(last));

            String nextId = service.generateNextTransactionId();

            assertThat(nextId).isEqualTo("0000000000000043");
        }

        @Test
        @DisplayName("7.4: ID is zero-padded to 16 digits")
        void zeroPadded() {
            String id = service.generateNextTransactionId();
            assertThat(id).hasSize(16);
            assertThat(id).matches("\\d{16}");
        }
    }

    // ---------------------------------------------------------------
    // §8 Transaction Record Construction
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 8: Transaction Record Construction")
    class TranRecordConstruction {

        @Test
        @DisplayName("8.1-8.13: All fields match COBOL spec")
        void allFieldsCorrect() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            service.processPayment("00000000001", "Y");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction txn = captor.getValue();

            assertThat(txn.getTranId()).isEqualTo("0000000000000001");
            assertThat(txn.getTranTypeCd()).isEqualTo("02");
            assertThat(txn.getTranCatCd()).isEqualTo(2);
            assertThat(txn.getTranSource()).isEqualTo("POS TERM");
            assertThat(txn.getTranDesc()).isEqualTo("BILL PAYMENT - ONLINE");
            assertThat(txn.getTranAmt()).isEqualByComparingTo("1500.00");
            assertThat(txn.getCardNum()).isEqualTo("4111111111111111");
            assertThat(txn.getMerchantId()).isEqualTo(999999999L);
            assertThat(txn.getMerchantName()).isEqualTo("BILL PAYMENT");
            assertThat(txn.getMerchantCity()).isEqualTo("N/A");
            assertThat(txn.getMerchantZip()).isEqualTo("N/A");
            assertThat(txn.getOrigTs()).isNotNull();
            assertThat(txn.getProcTs()).isEqualTo(txn.getOrigTs());
        }

        @Test
        @DisplayName("8.12-8.13: Timestamps follow YYYY-MM-DD HH:MM:SS.000000 format")
        void timestampFormat() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            service.processPayment("00000000001", "Y");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            String ts = captor.getValue().getOrigTs();
            assertThat(ts).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}");
        }
    }

    // ---------------------------------------------------------------
    // §9 Account Balance Update
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 9: Account Balance Update")
    class BalanceUpdate {

        @Test
        @DisplayName("9.1: Balance zeroed after full payment")
        void balanceZeroedAfterPayment() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            BillPayResponse resp = service.processPayment("00000000001", "Y");

            assertThat(resp.previousBalance()).isEqualByComparingTo("1500.00");
            assertThat(resp.newBalance()).isEqualByComparingTo("0.00");

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("9.2: Account is persisted after balance update")
        void accountPersisted() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            service.processPayment("00000000001", "Y");

            verify(accountRepository).save(any(Account.class));
        }
    }

    // ---------------------------------------------------------------
    // §10 Transaction Write Outcomes
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Rule 10: Transaction Write Outcomes")
    class TransactionWriteOutcomes {

        @Test
        @DisplayName("10.1: Success message includes transaction ID")
        void successMessage() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById(any())).thenReturn(false);

            BillPayResponse resp = service.processPayment("00000000001", "Y");

            assertThat(resp.message()).startsWith("Payment successful. Your Transaction ID is ");
            assertThat(resp.message()).endsWith(".");
        }

        @Test
        @DisplayName("10.2: Duplicate transaction ID throws DuplicateTransactionException")
        void duplicateTransactionId() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));
            when(cardXrefRepository.findByAcctId("00000000001")).thenReturn(Optional.of(testXref));
            when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
            when(transactionRepository.existsById("0000000000000001")).thenReturn(true);

            assertThatThrownBy(() -> service.processPayment("00000000001", "Y"))
                    .isInstanceOf(DuplicateTransactionException.class)
                    .hasMessageContaining("Tran ID already exist");
        }
    }

    // ---------------------------------------------------------------
    // Balance Inquiry (GET endpoint)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Balance Inquiry")
    class BalanceInquiry {

        @Test
        @DisplayName("Returns balance and confirmation prompt")
        void lookupBalance() {
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            BalanceInquiryResponse resp = service.lookupBalance("00000000001");

            assertThat(resp.accountId()).isEqualTo("00000000001");
            assertThat(resp.currentBalance()).isEqualByComparingTo("1500.00");
            assertThat(resp.message()).isEqualTo("Confirm to make a bill payment");
        }

        @Test
        @DisplayName("Account not found on lookup")
        void lookupNotFound() {
            when(accountRepository.findById("99999999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.lookupBalance("99999999999"))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("Zero balance on lookup")
        void lookupZeroBalance() {
            testAccount.setCurrentBalance(BigDecimal.ZERO);
            when(accountRepository.findById("00000000001")).thenReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> service.lookupBalance("00000000001"))
                    .isInstanceOf(NothingToPayException.class);
        }
    }
}
