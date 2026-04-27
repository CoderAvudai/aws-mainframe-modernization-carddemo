package com.aws.carddemo.service;

import com.aws.carddemo.dto.InterestCalculationResponse;
import com.aws.carddemo.dto.InterestResult;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.CardCrossReference;
import com.aws.carddemo.entity.DisclosureGroup;
import com.aws.carddemo.entity.DisclosureGroupId;
import com.aws.carddemo.entity.Transaction;
import com.aws.carddemo.entity.TransactionCategoryBalance;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardCrossReferenceRepository;
import com.aws.carddemo.repository.DisclosureGroupRepository;
import com.aws.carddemo.repository.TransactionCategoryBalanceRepository;
import com.aws.carddemo.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Converted from COBOL program CBACT04C.cbl — Interest Calculator.
 *
 * Original COBOL logic:
 *   1. Reads TCATBAL file sequentially (transaction category balances)
 *   2. For each new account encountered:
 *      - Updates the previous account's balance with accumulated interest
 *      - Loads account data and cross-reference (card number)
 *   3. For each category balance record:
 *      - Looks up interest rate from disclosure group
 *      - Falls back to "DEFAULT" group if not found
 *      - Computes: monthlyInterest = (balance * rate) / 1200
 *      - Generates an interest transaction record
 *   4. After all records, updates the last account
 */
@Service
public class InterestCalculationService {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationService.class);

    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");
    private static final String DEFAULT_GROUP_ID = "DEFAULT";
    private static final String INTEREST_TYPE_CD = "01";
    private static final int INTEREST_CAT_CD = 5;
    private static final String INTEREST_SOURCE = "System";
    private static final DateTimeFormatter DB2_TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS0000");

    private final TransactionCategoryBalanceRepository tcatBalRepo;
    private final AccountRepository accountRepo;
    private final CardCrossReferenceRepository xrefRepo;
    private final DisclosureGroupRepository disclosureRepo;
    private final TransactionRepository transactionRepo;

    public InterestCalculationService(
            TransactionCategoryBalanceRepository tcatBalRepo,
            AccountRepository accountRepo,
            CardCrossReferenceRepository xrefRepo,
            DisclosureGroupRepository disclosureRepo,
            TransactionRepository transactionRepo) {
        this.tcatBalRepo = tcatBalRepo;
        this.accountRepo = accountRepo;
        this.xrefRepo = xrefRepo;
        this.disclosureRepo = disclosureRepo;
        this.transactionRepo = transactionRepo;
    }

    @Transactional
    public InterestCalculationResponse calculateInterest(String processingDate) {
        log.info("START OF EXECUTION OF INTEREST CALCULATION, date={}", processingDate);

        List<TransactionCategoryBalance> allBalances =
                tcatBalRepo.findAllByOrderByAcctIdAscTypeCdAscCatCdAsc();

        Map<Long, List<TransactionCategoryBalance>> balancesByAccount = new LinkedHashMap<>();
        for (TransactionCategoryBalance bal : allBalances) {
            balancesByAccount.computeIfAbsent(bal.getAcctId(), k -> new ArrayList<>()).add(bal);
        }

        AtomicInteger tranIdSuffix = new AtomicInteger(0);
        List<InterestResult> results = new ArrayList<>();
        int recordCount = 0;

        for (Map.Entry<Long, List<TransactionCategoryBalance>> entry : balancesByAccount.entrySet()) {
            Long acctId = entry.getKey();
            List<TransactionCategoryBalance> balances = entry.getValue();

            InterestResult result = processAccount(acctId, balances, processingDate, tranIdSuffix);
            if (result != null) {
                results.add(result);
            }
            recordCount += balances.size();
        }

        log.info("END OF EXECUTION OF INTEREST CALCULATION, records={}", recordCount);

        return new InterestCalculationResponse("SUCCESS", processingDate, recordCount, results);
    }

    @Transactional
    public InterestCalculationResponse calculateInterestForAccount(
            Long accountId, String processingDate) {
        log.info("Calculating interest for account={}, date={}", accountId, processingDate);

        List<TransactionCategoryBalance> balances =
                tcatBalRepo.findByAcctIdOrderByTypeCdAscCatCdAsc(accountId);

        if (balances.isEmpty()) {
            return new InterestCalculationResponse(
                    "NO_DATA", processingDate, 0, List.of());
        }

        AtomicInteger tranIdSuffix = new AtomicInteger(0);
        InterestResult result = processAccount(accountId, balances, processingDate, tranIdSuffix);

        List<InterestResult> results = result != null ? List.of(result) : List.of();
        return new InterestCalculationResponse(
                "SUCCESS", processingDate, balances.size(), results);
    }

    private InterestResult processAccount(
            Long acctId,
            List<TransactionCategoryBalance> balances,
            String processingDate,
            AtomicInteger tranIdSuffix) {

        Optional<Account> accountOpt = accountRepo.findById(acctId);
        if (accountOpt.isEmpty()) {
            log.warn("ACCOUNT NOT FOUND: {}", acctId);
            return null;
        }
        Account account = accountOpt.get();

        Optional<CardCrossReference> xrefOpt = xrefRepo.findFirstByAcctId(acctId);
        if (xrefOpt.isEmpty()) {
            log.warn("XREF NOT FOUND FOR ACCOUNT: {}", acctId);
            return null;
        }
        String cardNum = xrefOpt.get().getCardNum();

        BigDecimal totalInterest = BigDecimal.ZERO;
        int txCount = 0;

        for (TransactionCategoryBalance catBal : balances) {
            BigDecimal intRate = lookupInterestRate(
                    account.getGroupId(), catBal.getTypeCd(), catBal.getCatCd());

            if (intRate.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal monthlyInterest = computeMonthlyInterest(catBal.getBalance(), intRate);
                totalInterest = totalInterest.add(monthlyInterest);

                Transaction tx = buildInterestTransaction(
                        processingDate, tranIdSuffix.incrementAndGet(),
                        acctId, cardNum, monthlyInterest);
                transactionRepo.save(tx);
                txCount++;
            }
        }

        updateAccountBalance(account, totalInterest);

        return new InterestResult(acctId, totalInterest, account.getCurrBal(), txCount);
    }

    /**
     * COBOL 1200-GET-INTEREST-RATE / 1200-A-GET-DEFAULT-INT-RATE:
     * Looks up the disclosure group by (groupId, typeCd, catCd).
     * Falls back to "DEFAULT" group if not found.
     */
    BigDecimal lookupInterestRate(String groupId, String typeCd, Integer catCd) {
        DisclosureGroupId key = new DisclosureGroupId(groupId, typeCd, catCd);
        Optional<DisclosureGroup> discOpt = disclosureRepo.findById(key);

        if (discOpt.isPresent()) {
            return discOpt.get().getIntRate();
        }

        DisclosureGroupId defaultKey = new DisclosureGroupId(DEFAULT_GROUP_ID, typeCd, catCd);
        Optional<DisclosureGroup> defaultOpt = disclosureRepo.findById(defaultKey);

        if (defaultOpt.isPresent()) {
            return defaultOpt.get().getIntRate();
        }

        log.warn("No disclosure group found for group={}, type={}, cat={}", groupId, typeCd, catCd);
        return BigDecimal.ZERO;
    }

    /**
     * COBOL 1300-COMPUTE-INTEREST:
     *   COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     */
    BigDecimal computeMonthlyInterest(BigDecimal balance, BigDecimal annualRate) {
        return balance.multiply(annualRate)
                .divide(TWELVE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * COBOL 1050-UPDATE-ACCOUNT:
     *   ADD WS-TOTAL-INT TO ACCT-CURR-BAL
     *   MOVE 0 TO ACCT-CURR-CYC-CREDIT
     *   MOVE 0 TO ACCT-CURR-CYC-DEBIT
     */
    private void updateAccountBalance(Account account, BigDecimal totalInterest) {
        account.setCurrBal(account.getCurrBal().add(totalInterest));
        account.setCurrCycCredit(BigDecimal.ZERO);
        account.setCurrCycDebit(BigDecimal.ZERO);
        accountRepo.save(account);
    }

    /**
     * COBOL 1300-B-WRITE-TX:
     * Builds an interest transaction record matching the COBOL TRAN-RECORD layout.
     */
    private Transaction buildInterestTransaction(
            String processingDate, int suffix,
            Long acctId, String cardNum, BigDecimal monthlyInterest) {

        Transaction tx = new Transaction();
        tx.setTranId(processingDate + String.format("%06d", suffix));
        tx.setTypeCd(INTEREST_TYPE_CD);
        tx.setCatCd(INTEREST_CAT_CD);
        tx.setSource(INTEREST_SOURCE);
        tx.setDescription("Int. for a/c " + acctId);
        tx.setAmount(monthlyInterest);
        tx.setMerchantId(0L);
        tx.setMerchantName("");
        tx.setMerchantCity("");
        tx.setMerchantZip("");
        tx.setCardNum(cardNum);

        String timestamp = LocalDateTime.now().format(DB2_TS_FORMATTER);
        tx.setOrigTs(timestamp);
        tx.setProcTs(timestamp);

        return tx;
    }
}
