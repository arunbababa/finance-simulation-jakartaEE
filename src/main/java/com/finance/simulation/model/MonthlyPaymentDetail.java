package com.finance.simulation.model;

import jakarta.json.bind.annotation.JsonbProperty;
import java.math.BigDecimal;

/**
 * 月次返済明細
 *
 * 各月の返済内訳を表します：
 * - 支払額（元金 + 利息）
 * - 元金返済分
 * - 利息分
 * - 返済後残高
 */
public class MonthlyPaymentDetail {

    /** 返済回数（何回目の支払いか） */
    @JsonbProperty("paymentNumber")
    private int paymentNumber;

    /** 返済年月（YYYY-MM形式） */
    @JsonbProperty("paymentMonth")
    private String paymentMonth;

    /** 今月の支払総額（元金 + 利息） */
    @JsonbProperty("totalPayment")
    private BigDecimal totalPayment;

    /** 元金返済分 */
    @JsonbProperty("principalPayment")
    private BigDecimal principalPayment;

    /** 利息分 */
    @JsonbProperty("interestPayment")
    private BigDecimal interestPayment;

    /** 支払前の残高 */
    @JsonbProperty("balanceBefore")
    private BigDecimal balanceBefore;

    /** 支払後の残高 */
    @JsonbProperty("balanceAfter")
    private BigDecimal balanceAfter;

    /** ボーナス払い分（該当月のみ） */
    @JsonbProperty("bonusPayment")
    private BigDecimal bonusPayment;

    /** 累計支払額 */
    @JsonbProperty("cumulativePayment")
    private BigDecimal cumulativePayment;

    /** 累計利息 */
    @JsonbProperty("cumulativeInterest")
    private BigDecimal cumulativeInterest;

    // ========================================
    // コンストラクタ
    // ========================================

    public MonthlyPaymentDetail() {
    }

    // ========================================
    // ビルダーパターン
    // ========================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MonthlyPaymentDetail detail = new MonthlyPaymentDetail();

        public Builder paymentNumber(int paymentNumber) {
            detail.paymentNumber = paymentNumber;
            return this;
        }

        public Builder paymentMonth(String paymentMonth) {
            detail.paymentMonth = paymentMonth;
            return this;
        }

        public Builder totalPayment(BigDecimal totalPayment) {
            detail.totalPayment = totalPayment;
            return this;
        }

        public Builder principalPayment(BigDecimal principalPayment) {
            detail.principalPayment = principalPayment;
            return this;
        }

        public Builder interestPayment(BigDecimal interestPayment) {
            detail.interestPayment = interestPayment;
            return this;
        }

        public Builder balanceBefore(BigDecimal balanceBefore) {
            detail.balanceBefore = balanceBefore;
            return this;
        }

        public Builder balanceAfter(BigDecimal balanceAfter) {
            detail.balanceAfter = balanceAfter;
            return this;
        }

        public Builder bonusPayment(BigDecimal bonusPayment) {
            detail.bonusPayment = bonusPayment;
            return this;
        }

        public Builder cumulativePayment(BigDecimal cumulativePayment) {
            detail.cumulativePayment = cumulativePayment;
            return this;
        }

        public Builder cumulativeInterest(BigDecimal cumulativeInterest) {
            detail.cumulativeInterest = cumulativeInterest;
            return this;
        }

        public MonthlyPaymentDetail build() {
            return detail;
        }
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public int getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(int paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public String getPaymentMonth() {
        return paymentMonth;
    }

    public void setPaymentMonth(String paymentMonth) {
        this.paymentMonth = paymentMonth;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment) {
        this.totalPayment = totalPayment;
    }

    public BigDecimal getPrincipalPayment() {
        return principalPayment;
    }

    public void setPrincipalPayment(BigDecimal principalPayment) {
        this.principalPayment = principalPayment;
    }

    public BigDecimal getInterestPayment() {
        return interestPayment;
    }

    public void setInterestPayment(BigDecimal interestPayment) {
        this.interestPayment = interestPayment;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public BigDecimal getBonusPayment() {
        return bonusPayment;
    }

    public void setBonusPayment(BigDecimal bonusPayment) {
        this.bonusPayment = bonusPayment;
    }

    public BigDecimal getCumulativePayment() {
        return cumulativePayment;
    }

    public void setCumulativePayment(BigDecimal cumulativePayment) {
        this.cumulativePayment = cumulativePayment;
    }

    public BigDecimal getCumulativeInterest() {
        return cumulativeInterest;
    }

    public void setCumulativeInterest(BigDecimal cumulativeInterest) {
        this.cumulativeInterest = cumulativeInterest;
    }
}
