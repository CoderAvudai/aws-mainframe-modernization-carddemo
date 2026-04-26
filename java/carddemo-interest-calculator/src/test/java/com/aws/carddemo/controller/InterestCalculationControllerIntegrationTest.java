package com.aws.carddemo.controller;

import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.CardCrossReference;
import com.aws.carddemo.entity.DisclosureGroup;
import com.aws.carddemo.entity.TransactionCategoryBalance;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardCrossReferenceRepository;
import com.aws.carddemo.repository.DisclosureGroupRepository;
import com.aws.carddemo.repository.TransactionCategoryBalanceRepository;
import com.aws.carddemo.repository.TransactionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterestCalculationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepo;
    @Autowired
    private TransactionCategoryBalanceRepository tcatBalRepo;
    @Autowired
    private CardCrossReferenceRepository xrefRepo;
    @Autowired
    private DisclosureGroupRepository disclosureRepo;
    @Autowired
    private TransactionRepository transactionRepo;

    @BeforeEach
    void setUp() {
        transactionRepo.deleteAll();
        tcatBalRepo.deleteAll();
        xrefRepo.deleteAll();
        disclosureRepo.deleteAll();
        accountRepo.deleteAll();
    }

    private void seedTestData() {
        // Account
        Account account = new Account();
        account.setAcctId(10000000001L);
        account.setActiveStatus("Y");
        account.setCurrBal(new BigDecimal("5000.00"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("5000.00"));
        account.setOpenDate("2020-01-01");
        account.setExpirationDate("2028-12-31");
        account.setReissueDate("2024-12-31");
        account.setCurrCycCredit(new BigDecimal("500.00"));
        account.setCurrCycDebit(new BigDecimal("300.00"));
        account.setAddrZip("10001");
        account.setGroupId("PREMIUM");
        accountRepo.save(account);

        // Card Cross-Reference
        CardCrossReference xref = new CardCrossReference();
        xref.setCardNum("4111222233334444");
        xref.setCustId(900001L);
        xref.setAcctId(10000000001L);
        xrefRepo.save(xref);

        // Disclosure Groups
        DisclosureGroup dg1 = new DisclosureGroup();
        dg1.setAcctGroupId("PREMIUM");
        dg1.setTranTypeCd("01");
        dg1.setTranCatCd(1);
        dg1.setIntRate(new BigDecimal("18.00"));
        disclosureRepo.save(dg1);

        DisclosureGroup dg2 = new DisclosureGroup();
        dg2.setAcctGroupId("PREMIUM");
        dg2.setTranTypeCd("02");
        dg2.setTranCatCd(1);
        dg2.setIntRate(new BigDecimal("24.00"));
        disclosureRepo.save(dg2);

        // Default fallback group
        DisclosureGroup dgDefault = new DisclosureGroup();
        dgDefault.setAcctGroupId("DEFAULT");
        dgDefault.setTranTypeCd("03");
        dgDefault.setTranCatCd(1);
        dgDefault.setIntRate(new BigDecimal("30.00"));
        disclosureRepo.save(dgDefault);

        // Transaction Category Balances
        TransactionCategoryBalance bal1 = new TransactionCategoryBalance();
        bal1.setAcctId(10000000001L);
        bal1.setTypeCd("01");
        bal1.setCatCd(1);
        bal1.setBalance(new BigDecimal("2000.00"));
        tcatBalRepo.save(bal1);

        TransactionCategoryBalance bal2 = new TransactionCategoryBalance();
        bal2.setAcctId(10000000001L);
        bal2.setTypeCd("02");
        bal2.setCatCd(1);
        bal2.setBalance(new BigDecimal("3000.00"));
        tcatBalRepo.save(bal2);
    }

    @Test
    @DisplayName("POST /api/interest/calculate — full batch run with seeded data")
    void calculateAll() throws Exception {
        seedTestData();

        mockMvc.perform(post("/api/interest/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processingDate\":\"2024-01-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.processingDate", is("2024-01-15")))
                .andExpect(jsonPath("$.recordsProcessed", is(2)))
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].accountId", is(10000000001L)))
                // 2000*18/1200=30 + 3000*24/1200=60 => 90.00
                .andExpect(jsonPath("$.results[0].totalInterest", comparesEqualTo(90.00)))
                .andExpect(jsonPath("$.results[0].updatedBalance", comparesEqualTo(5090.00)))
                .andExpect(jsonPath("$.results[0].transactionsGenerated", is(2)));

        // Verify account was updated in DB
        Account updated = accountRepo.findById(10000000001L).orElseThrow();
        assertEquals(new BigDecimal("5090.00"), updated.getCurrBal());
        assertEquals(0, updated.getCurrCycCredit().compareTo(BigDecimal.ZERO));
        assertEquals(0, updated.getCurrCycDebit().compareTo(BigDecimal.ZERO));

        // Verify transactions were created
        assertEquals(2, transactionRepo.count());
    }

    @Test
    @DisplayName("POST /api/interest/calculate/{accountId} — single-account run")
    void calculateForAccount() throws Exception {
        seedTestData();

        mockMvc.perform(post("/api/interest/calculate/10000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processingDate\":\"2024-02-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[0].totalInterest", comparesEqualTo(90.00)));
    }

    @Test
    @DisplayName("POST /api/interest/calculate — empty database returns zero records")
    void calculateWithNoData() throws Exception {
        mockMvc.perform(post("/api/interest/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processingDate\":\"2024-01-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.recordsProcessed", is(0)))
                .andExpect(jsonPath("$.results", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/interest/calculate — validation error when date missing")
    void validationErrorMissingDate() throws Exception {
        mockMvc.perform(post("/api/interest/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/interest/calculate — uses DEFAULT disclosure group as fallback")
    void defaultGroupFallback() throws Exception {
        // Seed account with group "UNKNOWN" — no matching disclosure
        Account account = new Account();
        account.setAcctId(20000000001L);
        account.setActiveStatus("Y");
        account.setCurrBal(new BigDecimal("1000.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("2500.00"));
        account.setOpenDate("2021-06-01");
        account.setExpirationDate("2029-06-01");
        account.setReissueDate("2025-06-01");
        account.setCurrCycCredit(BigDecimal.ZERO);
        account.setCurrCycDebit(BigDecimal.ZERO);
        account.setAddrZip("90210");
        account.setGroupId("UNKNOWN");
        accountRepo.save(account);

        CardCrossReference xref = new CardCrossReference();
        xref.setCardNum("5500998877665544");
        xref.setCustId(800001L);
        xref.setAcctId(20000000001L);
        xrefRepo.save(xref);

        // Only a DEFAULT disclosure group for type 03, cat 1
        DisclosureGroup dgDefault = new DisclosureGroup();
        dgDefault.setAcctGroupId("DEFAULT");
        dgDefault.setTranTypeCd("03");
        dgDefault.setTranCatCd(1);
        dgDefault.setIntRate(new BigDecimal("30.00"));
        disclosureRepo.save(dgDefault);

        // Balance with type 03, cat 1 — should match DEFAULT
        TransactionCategoryBalance bal = new TransactionCategoryBalance();
        bal.setAcctId(20000000001L);
        bal.setTypeCd("03");
        bal.setCatCd(1);
        bal.setBalance(new BigDecimal("1200.00"));
        tcatBalRepo.save(bal);

        mockMvc.perform(post("/api/interest/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processingDate\":\"2024-03-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(1)))
                // 1200 * 30 / 1200 = 30.00
                .andExpect(jsonPath("$.results[0].totalInterest", comparesEqualTo(30.00)))
                .andExpect(jsonPath("$.results[0].updatedBalance", comparesEqualTo(1030.00)));
    }
}
