package com.finance.simulation.model;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ローン返済シミュレーションのリクエストパラメータ
 */
public class LoanSimulationRequest {

    /** 借入金額（元本） */
    @NotNull(message = "借入金額は必須です")
    @DecimalMin(value = "1", message = "借入金額は1円以上である必要があります")
    @JsonbProperty("principal")
    private BigDecimal principal;

    /** 年利率（例: 0.15 = 15%） */
    @NotNull(message = "年利率は必須です")
    @DecimalMin(value = "0", message = "年利率は0%以上である必要があります")
    @DecimalMax(value = "0.20", message = "年利率は20%以下である必要があります（利息制限法）")
    @JsonbProperty("annualRate")
    private BigDecimal annualRate;

    /** 返済方式 */
    @NotNull(message = "返済方式は必須です")
    @JsonbProperty("repaymentType")
    private RepaymentType repaymentType;

    /** 返済期間（月数）- 分割払い・元利均等・元金均等で使用 */
    @Min(value = 1, message = "返済期間は1ヶ月以上である必要があります")
    @JsonbProperty("termMonths")
    private Integer termMonths;

    /** 月々の返済額 - リボ払い（定額方式）で使用 */
    @JsonbProperty("monthlyPayment")
    private BigDecimal monthlyPayment;

    /** 返済率 - リボ払い（定率方式）で使用（例: 0.03 = 残高の3%） */
    @JsonbProperty("repaymentRate")
    private BigDecimal repaymentRate;

    /** 残高スライドのテーブル種別 - リボ払い（残高スライド）で使用 */
    @JsonbProperty("balanceSlideType")
    private BalanceSlideType balanceSlideType;

    /** 分割回数 - 分割払いで使用（3, 6, 10, 12, 15, 18, 20, 24回など） */
    @JsonbProperty("installmentCount")
    private Integer installmentCount;

    /** ボーナス払い金額（オプション） */
    @JsonbProperty("bonusPayment")
    private BigDecimal bonusPayment;

    /** ボーナス月（1-12の配列、例: [7, 12]） */
    @JsonbProperty("bonusMonths")
    private int[] bonusMonths;

    // ========================================
    // コンストラクタ
    // ========================================

    public LoanSimulationRequest() {
    }

    // ========================================
    // ビルダーパターン
    // ========================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LoanSimulationRequest request = new LoanSimulationRequest();

        public Builder principal(BigDecimal principal) {
            request.principal = principal;
            return this;
        }

        public Builder annualRate(BigDecimal annualRate) {
            request.annualRate = annualRate;
            return this;
        }

        public Builder repaymentType(RepaymentType repaymentType) {
            request.repaymentType = repaymentType;
            return this;
        }

        public Builder termMonths(Integer termMonths) {
            request.termMonths = termMonths;
            return this;
        }

        public Builder monthlyPayment(BigDecimal monthlyPayment) {
            request.monthlyPayment = monthlyPayment;
            return this;
        }

        public Builder repaymentRate(BigDecimal repaymentRate) {
            request.repaymentRate = repaymentRate;
            return this;
        }

        public Builder balanceSlideType(BalanceSlideType balanceSlideType) {
            request.balanceSlideType = balanceSlideType;
            return this;
        }

        public Builder installmentCount(Integer installmentCount) {
            request.installmentCount = installmentCount;
            return this;
        }

        public Builder bonusPayment(BigDecimal bonusPayment) {
            request.bonusPayment = bonusPayment;
            return this;
        }

        public Builder bonusMonths(int[] bonusMonths) {
            request.bonusMonths = bonusMonths;
            return this;
        }

        public LoanSimulationRequest build() {
            return request;
        }
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public void setAnnualRate(BigDecimal annualRate) {
        this.annualRate = annualRate;
    }

    public RepaymentType getRepaymentType() {
        return repaymentType;
    }

    public void setRepaymentType(RepaymentType repaymentType) {
        this.repaymentType = repaymentType;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getRepaymentRate() {
        return repaymentRate;
    }

    public void setRepaymentRate(BigDecimal repaymentRate) {
        this.repaymentRate = repaymentRate;
    }

    public BalanceSlideType getBalanceSlideType() {
        return balanceSlideType;
    }

    public void setBalanceSlideType(BalanceSlideType balanceSlideType) {
        this.balanceSlideType = balanceSlideType;
    }

    public Integer getInstallmentCount() {
        return installmentCount;
    }

    public void setInstallmentCount(Integer installmentCount) {
        this.installmentCount = installmentCount;
    }

    public BigDecimal getBonusPayment() {
        return bonusPayment;
    }

    public void setBonusPayment(BigDecimal bonusPayment) {
        this.bonusPayment = bonusPayment;
    }

    public int[] getBonusMonths() {
        return bonusMonths;
    }

    public void setBonusMonths(int[] bonusMonths) {
        this.bonusMonths = bonusMonths;
    }
}
