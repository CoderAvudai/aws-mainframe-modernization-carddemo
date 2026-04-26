package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Maps to COBOL copybook CVTRA01Y — Transaction Category Balance (RECLN 50).
 * Original: TRAN-CAT-BAL-RECORD in CBACT04C.cbl
 */
@Entity
@Table(name = "tran_cat_balance")
@IdClass(TransactionCategoryBalanceId.class)
public class TransactionCategoryBalance {

    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Id
    @Column(name = "type_cd", length = 2, nullable = false)
    private String typeCd;

    @Id
    @Column(name = "cat_cd", nullable = false)
    private Integer catCd;

    @Column(name = "balance", precision = 11, scale = 2)
    private BigDecimal balance;

    public TransactionCategoryBalance() {
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public String getTypeCd() {
        return typeCd;
    }

    public void setTypeCd(String typeCd) {
        this.typeCd = typeCd;
    }

    public Integer getCatCd() {
        return catCd;
    }

    public void setCatCd(Integer catCd) {
        this.catCd = catCd;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
