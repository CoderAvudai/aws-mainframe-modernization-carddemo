package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps to COBOL copybook CVACT03Y — Card Cross-Reference (RECLN 50).
 * Original: CARD-XREF-RECORD in CBACT04C.cbl
 */
@Entity
@Table(name = "card_xref")
public class CardCrossReference {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "cust_id")
    private Long custId;

    @Column(name = "acct_id")
    private Long acctId;

    public CardCrossReference() {
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }
}
