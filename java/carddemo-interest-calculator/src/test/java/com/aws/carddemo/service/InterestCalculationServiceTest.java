package com.aws.carddemo.service;

import com.aws.carddemo.dto.InterestCalculationResponse;
import com.aws.carddemo.dto.InterestResult;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.CardCrossReference;
import com.aws.carddemo.entity.DisclosureGroup;
import com.aws.carddemo.entity.DisclosureGroupId;
import com.aws.carddemo.entity.TransactionCategoryBalance;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardCrossReferenceRepository;
import com.aws.carddemo.repository.DisclosureGroupRepository;
import com.aws.carddemo.repository.TransactionCategoryBalanceRepository;
import com.aws.carddemo.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestCalculationServiceTest {

    @Mock
    private TransactionCategoryBalanceRepository tcatBalRepo;
    @Mock
    private AccountRepository accountRepo;
    @Mock
    private CardCrossReferenceRepository xrefRepo;
    @Mock
    private DisclosureGroupRepository disclosureRepo;
    @Mock
    private TransactionRepository transactionRepo;

    @InjectMocks
    private InterestCalculationService service;

    private static final String PROCESSING_DATE = "2024-01-15";

    private Account createAccount(Long id, BigDecimal balance, String groupId) {
        Account a = new Account();
        a.setAcctId(id);
        a.setActiveStatus("Y");
        a.setCurrBal(balance);
        a.setCreditLimit(new BigDecimal("10000.00"));
        a.setCashCreditLimit(new BigDecimal("5000.00"));
        a.setOpenDate("2020-01-01");
        a.setExpirationDate("2028-12-31");
        a.setReissueDate("2024-12-31");
        a.setCurrCycCredit(new BigDecimal("500.00"));
        a.setCurrCycDebit(new BigDecimal("300.00"));
        a.setAddrZip("10001");
        a.setGroupId(groupId);
        return a;
    }

    private CardCrossReference createXref(Long acctId, String cardNum) {
        CardCrossReference x = new CardCrossReference();
        x.setCardNum(cardNum);
        x.setCustId(100001L);
        x.setAcctId(acctId);
        return x;
    }

    private TransactionCategoryBalance createTcatBal(Long acctId, String typeCd,
                                                      Integer catCd, BigDecimal balance) {
        TransactionCategoryBalance t = new TransactionCategoryBalance();
        t.setAcctId(acctId);
        t.setTypeCd(typeCd);
        t.setCatCd(catCd);
        t.setBalance(balance);
        return t;
    }

    private DisclosureGroup createDiscGroup(String groupId, String typeCd,
                                            Integer catCd, BigDecimal rate) {
        DisclosureGroup d = new DisclosureGroup();
        d.setAcctGroupId(groupId);
        d.setTranTypeCd(typeCd);
        d.setTranCatCd(catCd);
        d.setIntRate(rate);
        return d;
    }

    @Nested
    @DisplayName("computeMonthlyInterest — COBOL formula: (balance * rate) / 1200")
    class ComputeMonthlyInterestTests {

        @Test
        @DisplayName("Standard interest: $1000 balance at 18% APR = $15.00 monthly")
        void standardInterestCalculation() {
            BigDecimal balance = new BigDecimal("1000.00");
            BigDecimal rate = new BigDecimal("18.00");
            BigDecimal result = service.computeMonthlyInterest(balance, rate);
            assertEquals(new BigDecimal("15.00"), result);
        }

        @Test
        @DisplayName("Zero balance produces zero interest")
        void zeroBalance() {
            BigDecimal result = service.computeMonthlyInterest(BigDecimal.ZERO, new BigDecimal("18.00"));
            assertEquals(new BigDecimal("0.00"), result);
        }

        @Test
        @DisplayName("Zero rate produces zero interest")
        void zeroRate() {
            BigDecimal result = service.computeMonthlyInterest(new BigDecimal("5000.00"), BigDecimal.ZERO);
            assertEquals(new BigDecimal("0.00"), result);
        }

        @Test
        @DisplayName("Large balance: $999999999.99 at 24% = $19999999.99 monthly")
        void largeBalance() {
            BigDecimal balance = new BigDecimal("999999999.99");
            BigDecimal rate = new BigDecimal("24.00");
            BigDecimal result = service.computeMonthlyInterest(balance, rate);
            assertEquals(new BigDecimal("20000000.00"), result);
        }

        @Test
        @DisplayName("Fractional rate: $5000 at 12.75% = $53.13 monthly")
        void fractionalRate() {
            BigDecimal balance = new BigDecimal("5000.00");
            BigDecimal rate = new BigDecimal("12.75");
            BigDecimal result = service.computeMonthlyInterest(balance, rate);
            assertEquals(new BigDecimal("53.13"), result);
        }

        @Test
        @DisplayName("Negative balance: -$500 at 18% = -$7.50 monthly")
        void negativeBalance() {
            BigDecimal balance = new BigDecimal("-500.00");
            BigDecimal rate = new BigDecimal("18.00");
            BigDecimal result = service.computeMonthlyInterest(balance, rate);
            assertEquals(new BigDecimal("-7.50"), result);
        }
    }

    @Nested
    @DisplayName("lookupInterestRate — group lookup with DEFAULT fallback")
    class LookupInterestRateTests {

        @Test
        @DisplayName("Returns rate from specific group when found")
        void specificGroupFound() {
            DisclosureGroup dg = createDiscGroup("PLATINUM", "01", 1, new BigDecimal("12.00"));
            when(disclosureRepo.findById(new DisclosureGroupId("PLATINUM", "01", 1)))
                    .thenReturn(Optional.of(dg));

            BigDecimal rate = service.lookupInterestRate("PLATINUM", "01", 1);
            assertEquals(new BigDecimal("12.00"), rate);
        }

        @Test
        @DisplayName("Falls back to DEFAULT group when specific group not found")
        void fallsBackToDefault() {
            when(disclosureRepo.findById(new DisclosureGroupId("GOLD", "01", 1)))
                    .thenReturn(Optional.empty());

            DisclosureGroup defaultDg = createDiscGroup("DEFAULT", "01", 1, new BigDecimal("24.00"));
            when(disclosureRepo.findById(new DisclosureGroupId("DEFAULT", "01", 1)))
                    .thenReturn(Optional.of(defaultDg));

            BigDecimal rate = service.lookupInterestRate("GOLD", "01", 1);
            assertEquals(new BigDecimal("24.00"), rate);
        }

        @Test
        @DisplayName("Returns zero when neither specific nor DEFAULT group found")
        void noGroupFound() {
            when(disclosureRepo.findById(any())).thenReturn(Optional.empty());

            BigDecimal rate = service.lookupInterestRate("UNKNOWN", "99", 99);
            assertEquals(BigDecimal.ZERO, rate);
        }
    }

    @Nested
    @DisplayName("calculateInterest — full batch processing")
    class CalculateInterestTests {

        @Test
        @DisplayName("Processes single account with one category balance")
        void singleAccountSingleBalance() {
            Long acctId = 12345678901L;
            Account account = createAccount(acctId, new BigDecimal("1000.00"), "PREMIUM");
            CardCrossReference xref = createXref(acctId, "4111222233334444");
            TransactionCategoryBalance bal = createTcatBal(acctId, "01", 1, new BigDecimal("1000.00"));
            DisclosureGroup dg = createDiscGroup("PREMIUM", "01", 1, new BigDecimal("18.00"));

            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of(bal));
            when(accountRepo.findById(acctId)).thenReturn(Optional.of(account));
            when(xrefRepo.findFirstByAcctId(acctId)).thenReturn(Optional.of(xref));
            when(disclosureRepo.findById(new DisclosureGroupId("PREMIUM", "01", 1)))
                    .thenReturn(Optional.of(dg));

            InterestCalculationResponse response = service.calculateInterest(PROCESSING_DATE);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getRecordsProcessed());
            assertEquals(1, response.getResults().size());

            InterestResult result = response.getResults().get(0);
            assertEquals(acctId, result.getAccountId());
            assertEquals(new BigDecimal("15.00"), result.getTotalInterest());
            assertEquals(new BigDecimal("1015.00"), result.getUpdatedBalance());
            assertEquals(1, result.getTransactionsGenerated());

            verify(transactionRepo, times(1)).save(any());
            verify(accountRepo, times(1)).save(account);
        }

        @Test
        @DisplayName("Processes account with multiple category balances")
        void singleAccountMultipleBalances() {
            Long acctId = 11111111111L;
            Account account = createAccount(acctId, new BigDecimal("5000.00"), "STANDARD");
            CardCrossReference xref = createXref(acctId, "5500112233445566");

            TransactionCategoryBalance bal1 = createTcatBal(acctId, "01", 1, new BigDecimal("2000.00"));
            TransactionCategoryBalance bal2 = createTcatBal(acctId, "01", 2, new BigDecimal("3000.00"));

            DisclosureGroup dg1 = createDiscGroup("STANDARD", "01", 1, new BigDecimal("18.00"));
            DisclosureGroup dg2 = createDiscGroup("STANDARD", "01", 2, new BigDecimal("24.00"));

            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of(bal1, bal2));
            when(accountRepo.findById(acctId)).thenReturn(Optional.of(account));
            when(xrefRepo.findFirstByAcctId(acctId)).thenReturn(Optional.of(xref));
            when(disclosureRepo.findById(new DisclosureGroupId("STANDARD", "01", 1)))
                    .thenReturn(Optional.of(dg1));
            when(disclosureRepo.findById(new DisclosureGroupId("STANDARD", "01", 2)))
                    .thenReturn(Optional.of(dg2));

            InterestCalculationResponse response = service.calculateInterest(PROCESSING_DATE);

            assertEquals(1, response.getResults().size());
            InterestResult result = response.getResults().get(0);
            // 2000*18/1200 = 30.00, 3000*24/1200 = 60.00 => total = 90.00
            assertEquals(new BigDecimal("90.00"), result.getTotalInterest());
            assertEquals(new BigDecimal("5090.00"), result.getUpdatedBalance());
            assertEquals(2, result.getTransactionsGenerated());
        }

        @Test
        @DisplayName("Processes multiple accounts")
        void multipleAccounts() {
            Long acctId1 = 11111111111L;
            Long acctId2 = 22222222222L;

            Account account1 = createAccount(acctId1, new BigDecimal("1000.00"), "GRP1");
            Account account2 = createAccount(acctId2, new BigDecimal("2000.00"), "GRP1");

            CardCrossReference xref1 = createXref(acctId1, "4111000011110000");
            CardCrossReference xref2 = createXref(acctId2, "4222000022220000");

            TransactionCategoryBalance bal1 = createTcatBal(acctId1, "01", 1, new BigDecimal("1000.00"));
            TransactionCategoryBalance bal2 = createTcatBal(acctId2, "01", 1, new BigDecimal("2000.00"));

            DisclosureGroup dg = createDiscGroup("GRP1", "01", 1, new BigDecimal("12.00"));

            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of(bal1, bal2));
            when(accountRepo.findById(acctId1)).thenReturn(Optional.of(account1));
            when(accountRepo.findById(acctId2)).thenReturn(Optional.of(account2));
            when(xrefRepo.findFirstByAcctId(acctId1)).thenReturn(Optional.of(xref1));
            when(xrefRepo.findFirstByAcctId(acctId2)).thenReturn(Optional.of(xref2));
            when(disclosureRepo.findById(new DisclosureGroupId("GRP1", "01", 1)))
                    .thenReturn(Optional.of(dg));

            InterestCalculationResponse response = service.calculateInterest(PROCESSING_DATE);

            assertEquals(2, response.getResults().size());
            assertEquals(2, response.getRecordsProcessed());

            // Account1: 1000*12/1200 = 10.00
            assertEquals(new BigDecimal("10.00"), response.getResults().get(0).getTotalInterest());
            // Account2: 2000*12/1200 = 20.00
            assertEquals(new BigDecimal("20.00"), response.getResults().get(1).getTotalInterest());
        }

        @Test
        @DisplayName("Skips account when not found in database")
        void accountNotFound() {
            Long acctId = 99999999999L;
            TransactionCategoryBalance bal = createTcatBal(acctId, "01", 1, new BigDecimal("1000.00"));

            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of(bal));
            when(accountRepo.findById(acctId)).thenReturn(Optional.empty());

            InterestCalculationResponse response = service.calculateInterest(PROCESSING_DATE);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getRecordsProcessed());
            assertTrue(response.getResults().isEmpty());
            verify(transactionRepo, never()).save(any());
        }

        @Test
        @DisplayName("Skips interest when rate is zero")
        void zeroInterestRate() {
            Long acctId = 11111111111L;
            Account account = createAccount(acctId, new BigDecimal("5000.00"), "NOINT");
            CardCrossReference xref = createXref(acctId, "4111111111111111");
            TransactionCategoryBalance bal = createTcatBal(acctId, "01", 1, new BigDecimal("5000.00"));

            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of(bal));
            when(accountRepo.findById(acctId)).thenReturn(Optional.of(account));
            when(xrefRepo.findFirstByAcctId(acctId)).thenReturn(Optional.of(xref));
            when(disclosureRepo.findById(any())).thenReturn(Optional.empty());

            InterestCalculationResponse response = service.calculateInterest(PROCESSING_DATE);

            InterestResult result = response.getResults().get(0);
            assertEquals(BigDecimal.ZERO, result.getTotalInterest());
            assertEquals(new BigDecimal("5000.00"), result.getUpdatedBalance());
            assertEquals(0, result.getTransactionsGenerated());
            verify(transactionRepo, never()).save(any());
        }

        @Test
        @DisplayName("Returns empty results when no category balances exist")
        void noBalanceRecords() {
            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of());

            InterestCalculationResponse response = service.calculateInterest(PROCESSING_DATE);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(0, response.getRecordsProcessed());
            assertTrue(response.getResults().isEmpty());
        }

        @Test
        @DisplayName("Account cycle credits/debits reset to zero after update")
        void accountCycleFieldsReset() {
            Long acctId = 11111111111L;
            Account account = createAccount(acctId, new BigDecimal("1000.00"), "GRP1");
            account.setCurrCycCredit(new BigDecimal("500.00"));
            account.setCurrCycDebit(new BigDecimal("300.00"));

            CardCrossReference xref = createXref(acctId, "4111111111111111");
            TransactionCategoryBalance bal = createTcatBal(acctId, "01", 1, new BigDecimal("1000.00"));
            DisclosureGroup dg = createDiscGroup("GRP1", "01", 1, new BigDecimal("12.00"));

            when(tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc())
                    .thenReturn(List.of(bal));
            when(accountRepo.findById(acctId)).thenReturn(Optional.of(account));
            when(xrefRepo.findFirstByAcctId(acctId)).thenReturn(Optional.of(xref));
            when(disclosureRepo.findById(new DisclosureGroupId("GRP1", "01", 1)))
                    .thenReturn(Optional.of(dg));

            service.calculateInterest(PROCESSING_DATE);

            assertEquals(BigDecimal.ZERO, account.getCurrCycCredit());
            assertEquals(BigDecimal.ZERO, account.getCurrCycDebit());
        }
    }

    @Nested
    @DisplayName("calculateInterestForAccount — single-account processing")
    class CalculateInterestForAccountTests {

        @Test
        @DisplayName("Returns NO_DATA when no balances exist for account")
        void noDataForAccount() {
            Long acctId = 11111111111L;
            when(tcatBalRepo.findByAcctIdOrderByTypeCdAscCatCdAsc(acctId))
                    .thenReturn(List.of());

            InterestCalculationResponse response =
                    service.calculateInterestForAccount(acctId, PROCESSING_DATE);

            assertEquals("NO_DATA", response.getStatus());
            assertEquals(0, response.getRecordsProcessed());
            assertTrue(response.getResults().isEmpty());
        }

        @Test
        @DisplayName("Calculates interest for a specific account")
        void calculatesForSpecificAccount() {
            Long acctId = 12345678901L;
            Account account = createAccount(acctId, new BigDecimal("3000.00"), "VIP");
            CardCrossReference xref = createXref(acctId, "4999888877776666");
            TransactionCategoryBalance bal = createTcatBal(acctId, "02", 3, new BigDecimal("3000.00"));
            DisclosureGroup dg = createDiscGroup("VIP", "02", 3, new BigDecimal("6.00"));

            when(tcatBalRepo.findByAcctIdOrderByTypeCdAscCatCdAsc(acctId))
                    .thenReturn(List.of(bal));
            when(accountRepo.findById(acctId)).thenReturn(Optional.of(account));
            when(xrefRepo.findFirstByAcctId(acctId)).thenReturn(Optional.of(xref));
            when(disclosureRepo.findById(new DisclosureGroupId("VIP", "02", 3)))
                    .thenReturn(Optional.of(dg));

            InterestCalculationResponse response =
                    service.calculateInterestForAccount(acctId, PROCESSING_DATE);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getRecordsProcessed());
            assertNotNull(response.getResults());
            assertEquals(1, response.getResults().size());

            InterestResult result = response.getResults().get(0);
            // 3000 * 6 / 1200 = 15.00
            assertEquals(new BigDecimal("15.00"), result.getTotalInterest());
            assertEquals(new BigDecimal("3015.00"), result.getUpdatedBalance());
        }
    }
}
