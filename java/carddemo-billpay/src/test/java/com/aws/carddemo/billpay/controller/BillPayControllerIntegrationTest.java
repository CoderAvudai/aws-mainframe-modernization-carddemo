package com.aws.carddemo.billpay.controller;

import com.aws.carddemo.billpay.model.Account;
import com.aws.carddemo.billpay.model.CardXref;
import com.aws.carddemo.billpay.repository.AccountRepository;
import com.aws.carddemo.billpay.repository.CardXrefRepository;
import com.aws.carddemo.billpay.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillPayControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private AccountRepository accountRepo;
    @Autowired private CardXrefRepository xrefRepo;
    @Autowired private TransactionRepository txnRepo;

    @BeforeEach
    void setUp() {
        txnRepo.deleteAll();
        xrefRepo.deleteAll();
        accountRepo.deleteAll();

        Account acct = new Account();
        acct.setAcctId("00000000001");
        acct.setActiveStatus("Y");
        acct.setCurrentBalance(new BigDecimal("2500.50"));
        accountRepo.save(acct);

        CardXref xref = new CardXref();
        xref.setCardNum("4111111111111111");
        xref.setCustId("000000001");
        xref.setAcctId("00000000001");
        xrefRepo.save(xref);
    }

    @Test
    @DisplayName("GET balance returns current balance and confirmation prompt")
    void getBalance() throws Exception {
        mvc.perform(get("/api/v1/billpay/accounts/00000000001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is("00000000001")))
                .andExpect(jsonPath("$.currentBalance", is(2500.50)))
                .andExpect(jsonPath("$.message", is("Confirm to make a bill payment")));
    }

    @Test
    @DisplayName("GET balance – account not found returns 404")
    void getBalanceNotFound() throws Exception {
        mvc.perform(get("/api/v1/billpay/accounts/99999999999/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Account ID NOT found")));
    }

    @Test
    @DisplayName("POST with blank confirm returns confirmation prompt")
    void postBlankConfirm() throws Exception {
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Confirm to make a bill payment")))
                .andExpect(jsonPath("$.transactionId").value(nullValue()));
    }

    @Test
    @DisplayName("POST with confirm=N cancels payment")
    void postConfirmNo() throws Exception {
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": "N"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Payment cancelled")));
    }

    @Test
    @DisplayName("POST with confirm=Y executes payment, zeroes balance")
    void postConfirmYes() throws Exception {
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": "Y"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Payment successful")))
                .andExpect(jsonPath("$.previousBalance", is(2500.50)))
                .andExpect(jsonPath("$.newBalance", is(0.0)));
    }

    @Test
    @DisplayName("POST with empty accountId returns 400")
    void postEmptyAccountId() throws Exception {
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "", "confirm": "Y"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Acct ID can NOT be empty")));
    }

    @Test
    @DisplayName("POST with invalid confirm returns 400")
    void postInvalidConfirm() throws Exception {
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": "X"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid value")));
    }

    @Test
    @DisplayName("POST payment on zero balance account returns 422")
    void postZeroBalance() throws Exception {
        Account acct = accountRepo.findById("00000000001").orElseThrow();
        acct.setCurrentBalance(BigDecimal.ZERO);
        accountRepo.save(acct);

        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": "Y"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", containsString("nothing to pay")));
    }

    @Test
    @DisplayName("Full flow: inquiry → confirm → payment → second payment fails")
    void fullFlow() throws Exception {
        // Step 1: inquiry
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Confirm to make a bill payment")));

        // Step 2: confirm payment
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": "Y"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Payment successful")))
                .andExpect(jsonPath("$.newBalance", is(0.0)));

        // Step 3: second payment should fail – nothing to pay
        mvc.perform(post("/api/v1/billpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "00000000001", "confirm": "Y"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", containsString("nothing to pay")));
    }
}
