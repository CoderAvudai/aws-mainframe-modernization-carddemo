package com.carddemo.billpay.web;

import com.carddemo.billpay.domain.Account;
import com.carddemo.billpay.domain.CardXref;
import com.carddemo.billpay.repo.InMemoryAccountRepository;
import com.carddemo.billpay.repo.InMemoryCardXrefRepository;
import com.carddemo.billpay.repo.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP coverage. These tests exercise the Spring Web layer and the
 * {@link GlobalExceptionHandler} mapping of COBIL00C error paths onto HTTP
 * status codes.
 */
@SpringBootTest
class BillPaymentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private InMemoryAccountRepository accountRepo;

    @Autowired
    private InMemoryCardXrefRepository xrefRepo;

    @Autowired
    private InMemoryTransactionRepository tranRepo;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        accountRepo.clear();
        xrefRepo.clear();
        tranRepo.clear();
    }

    private void seed(BigDecimal balance) {
        accountRepo.seed(new Account(
                "00000000010", "Y", balance,
                new BigDecimal("10000.00"), new BigDecimal("2000.00"),
                "2022-01-01", "2027-01-01", "2027-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO, "12345", "GRP01"));
        xrefRepo.seed(new CardXref("4111111111111111", "000000001", "00000000010"));
    }

    @Test
    @DisplayName("POST /api/billpay with blank CONFIRM returns 200 and balance")
    void inquiryReturnsBalance() throws Exception {
        seed(new BigDecimal("75.00"));
        mvc.perform(post("/api/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"00000000010\",\"confirm\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.currentBalance").value(75.00))
                .andExpect(jsonPath("$.message").value("Confirm to make a bill payment..."));
    }

    @Test
    @DisplayName("POST /api/billpay with CONFIRM=Y completes the payment (200)")
    void paymentSucceeds() throws Exception {
        seed(new BigDecimal("25.00"));
        mvc.perform(post("/api/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"00000000010\",\"confirm\":\"Y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_SUCCESSFUL"))
                .andExpect(jsonPath("$.transactionId").value("0000000000000001"))
                .andExpect(jsonPath("$.currentBalance").value(0.0));
    }

    @Test
    @DisplayName("POST /api/billpay with blank account id returns 400 (R11)")
    void blankAccountIdReturns400() throws Exception {
        mvc.perform(post("/api/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"\",\"confirm\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Acct ID can NOT be empty..."));
    }

    @Test
    @DisplayName("POST /api/billpay with invalid CONFIRM returns 400 (R12)")
    void invalidConfirmReturns400() throws Exception {
        mvc.perform(post("/api/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"00000000010\",\"confirm\":\"Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value. Valid values are (Y/N)..."));
    }

    @Test
    @DisplayName("POST /api/billpay with unknown account returns 404 (R28)")
    void unknownAccountReturns404() throws Exception {
        mvc.perform(post("/api/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"99999999999\",\"confirm\":null}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account ID NOT found..."));
    }

    @Test
    @DisplayName("POST /api/billpay/clear returns blank screen (PF4)")
    void clearReturnsBlank() throws Exception {
        mvc.perform(post("/api/billpay/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.accountId").doesNotExist());
    }
}
