package com.finance.simulation.model;

import jakarta.json.bind.annotation.JsonbProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * ローン返済シミュレーションのレスポンス
 *
 * シミュレーション結果の全体サマリーと月次明細を含みます。
 */
public class LoanSimulationResponse {

    // ========================================
    // サマリー情報
    // ========================================

    /** 借入金額（元本） */
    @JsonbProperty("principal")
    private BigDecimal principal;

    /** 適用金利（年率） */
    @JsonbProperty("annualRate")
    private BigDecimal annualRate;

    /** 返済方式 */
    @JsonbProperty("repaymentType")
    private RepaymentType repaymentType;

    /** 返済方式の説明 */
    @JsonbProperty("repaymentTypeDescription")
    private String repaymentTypeDescription;

    /** 総返済額 */
    @JsonbProperty("totalPayment")
    private BigDecimal totalPayment;

    /** 総利息額 */
    @JsonbProperty("totalInterest")
    private BigDecimal totalInterest;

    /** 返済期間（月数） */
    @JsonbProperty("totalMonths")
    private int totalMonths;

    /** 返済期間（年と月の表示用） */
    @JsonbProperty("termDisplay")
    private String termDisplay;

    /** 月々の返済額（元利均等の場合など） */
    @JsonbProperty("monthlyPayment")
    private BigDecimal monthlyPayment;

    /** 初回返済額 */
    @JsonbProperty("firstPayment")
    private BigDecimal firstPayment;

    /** 最終回返済額 */
    @JsonbProperty("lastPayment")
    private BigDecimal lastPayment;

    /** 実質年率（APR: Annual Percentage Rate） */
    @JsonbProperty("effectiveAnnualRate")
    private BigDecimal effectiveAnnualRate;

    // ========================================
    // 法的情報
    // ========================================

    /** 利息制限法上の上限金利 */
    @JsonbProperty("legalMaxRate")
    private BigDecimal legalMaxRate;

    /** 上限金利超過の警告 */
    @JsonbProperty("rateWarning")
    private String rateWarning;

    /** 適用法令 */
    @JsonbProperty("applicableLaws")
    private List<String> applicableLaws;

    // ========================================
    // 月次明細
    // ========================================

    /** 月次返済明細リスト */
    @JsonbProperty("monthlyDetails")
    private List<MonthlyPaymentDetail> monthlyDetails;

    // ========================================
    // コンストラクタ
    // ========================================

    public LoanSimulationResponse() {
    }

    // ========================================
    // ビルダーパターン
    // ========================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LoanSimulationResponse response = new LoanSimulationResponse();

        public Builder principal(BigDecimal principal) {
            response.principal = principal;
            return this;
        }

        public Builder annualRate(BigDecimal annualRate) {
            response.annualRate = annualRate;
            return this;
        }

        public Builder repaymentType(RepaymentType repaymentType) {
            response.repaymentType = repaymentType;
            response.repaymentTypeDescription = repaymentType.getDescription();
            return this;
        }

        public Builder totalPayment(BigDecimal totalPayment) {
            response.totalPayment = totalPayment;
            return this;
        }

        public Builder totalInterest(BigDecimal totalInterest) {
            response.totalInterest = totalInterest;
            return this;
        }

        public Builder totalMonths(int totalMonths) {
            response.totalMonths = totalMonths;
            int years = totalMonths / 12;
            int months = totalMonths % 12;
            if (years > 0 && months > 0) {
                response.termDisplay = years + "年" + months + "ヶ月";
            } else if (years > 0) {
                response.termDisplay = years + "年";
            } else {
                response.termDisplay = months + "ヶ月";
            }
            return this;
        }

        public Builder monthlyPayment(BigDecimal monthlyPayment) {
            response.monthlyPayment = monthlyPayment;
            return this;
        }

        public Builder firstPayment(BigDecimal firstPayment) {
            response.firstPayment = firstPayment;
            return this;
        }

        public Builder lastPayment(BigDecimal lastPayment) {
            response.lastPayment = lastPayment;
            return this;
        }

        public Builder effectiveAnnualRate(BigDecimal effectiveAnnualRate) {
            response.effectiveAnnualRate = effectiveAnnualRate;
            return this;
        }

        public Builder legalMaxRate(BigDecimal legalMaxRate) {
            response.legalMaxRate = legalMaxRate;
            return this;
        }

        public Builder rateWarning(String rateWarning) {
            response.rateWarning = rateWarning;
            return this;
        }

        public Builder applicableLaws(List<String> applicableLaws) {
            response.applicableLaws = applicableLaws;
            return this;
        }

        public Builder monthlyDetails(List<MonthlyPaymentDetail> monthlyDetails) {
            response.monthlyDetails = monthlyDetails;
            return this;
        }

        public LoanSimulationResponse build() {
            return response;
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

    public String getRepaymentTypeDescription() {
        return repaymentTypeDescription;
    }

    public void setRepaymentTypeDescription(String repaymentTypeDescription) {
        this.repaymentTypeDescription = repaymentTypeDescription;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment) {
        this.totalPayment = totalPayment;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public int getTotalMonths() {
        return totalMonths;
    }

    public void setTotalMonths(int totalMonths) {
        this.totalMonths = totalMonths;
    }

    public String getTermDisplay() {
        return termDisplay;
    }

    public void setTermDisplay(String termDisplay) {
        this.termDisplay = termDisplay;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getFirstPayment() {
        return firstPayment;
    }

    public void setFirstPayment(BigDecimal firstPayment) {
        this.firstPayment = firstPayment;
    }

    public BigDecimal getLastPayment() {
        return lastPayment;
    }

    public void setLastPayment(BigDecimal lastPayment) {
        this.lastPayment = lastPayment;
    }

    public BigDecimal getEffectiveAnnualRate() {
        return effectiveAnnualRate;
    }

    public void setEffectiveAnnualRate(BigDecimal effectiveAnnualRate) {
        this.effectiveAnnualRate = effectiveAnnualRate;
    }

    public BigDecimal getLegalMaxRate() {
        return legalMaxRate;
    }

    public void setLegalMaxRate(BigDecimal legalMaxRate) {
        this.legalMaxRate = legalMaxRate;
    }

    public String getRateWarning() {
        return rateWarning;
    }

    public void setRateWarning(String rateWarning) {
        this.rateWarning = rateWarning;
    }

    public List<String> getApplicableLaws() {
        return applicableLaws;
    }

    public void setApplicableLaws(List<String> applicableLaws) {
        this.applicableLaws = applicableLaws;
    }

    public List<MonthlyPaymentDetail> getMonthlyDetails() {
        return monthlyDetails;
    }

    public void setMonthlyDetails(List<MonthlyPaymentDetail> monthlyDetails) {
        this.monthlyDetails = monthlyDetails;
    }
}
