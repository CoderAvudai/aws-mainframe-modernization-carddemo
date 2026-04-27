package com.aws.carddemo.billpay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Maps to COBOL copybook CVTRA05Y – TRAN-RECORD (TRANSACT VSAM file).
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "tran_id", length = 16, nullable = false)
    private String tranId;

    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    @Column(name = "tran_cat_cd")
    private int tranCatCd;

    @Column(name = "tran_source", length = 10)
    private String tranSource;

    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    @Column(name = "tran_amt", precision = 11, scale = 2)
    private BigDecimal tranAmt;

    @Column(name = "merchant_id")
    private long merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "card_num", length = 16)
    private String cardNum;

    @Column(name = "orig_ts", length = 26)
    private String origTs;

    @Column(name = "proc_ts", length = 26)
    private String procTs;

    public Transaction() {}

    public String getTranId() { return tranId; }
    public void setTranId(String tranId) { this.tranId = tranId; }

    public String getTranTypeCd() { return tranTypeCd; }
    public void setTranTypeCd(String tranTypeCd) { this.tranTypeCd = tranTypeCd; }

    public int getTranCatCd() { return tranCatCd; }
    public void setTranCatCd(int tranCatCd) { this.tranCatCd = tranCatCd; }

    public String getTranSource() { return tranSource; }
    public void setTranSource(String tranSource) { this.tranSource = tranSource; }

    public String getTranDesc() { return tranDesc; }
    public void setTranDesc(String tranDesc) { this.tranDesc = tranDesc; }

    public BigDecimal getTranAmt() { return tranAmt; }
    public void setTranAmt(BigDecimal tranAmt) { this.tranAmt = tranAmt; }

    public long getMerchantId() { return merchantId; }
    public void setMerchantId(long merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }

    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }

    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }

    public String getOrigTs() { return origTs; }
    public void setOrigTs(String origTs) { this.origTs = origTs; }

    public String getProcTs() { return procTs; }
    public void setProcTs(String procTs) { this.procTs = procTs; }
}
