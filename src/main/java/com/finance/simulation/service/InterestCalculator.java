package com.finance.simulation.service;

import com.finance.simulation.model.LegalConstants;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 利息計算サービス
 *
 * 【計算方法について】
 *
 * 1. 月利の計算
 *    月利 = 年利 ÷ 12
 *
 * 2. 日割り計算（実日数計算）
 *    日利 = 年利 ÷ 365
 *    利息 = 元金 × 日利 × 日数
 *
 * 3. 元利均等返済の月々返済額
 *    PMT = P × r × (1 + r)^n ÷ ((1 + r)^n - 1)
 *    P: 元本, r: 月利, n: 返済回数
 */
@ApplicationScoped
public class InterestCalculator {

    /**
     * 利息制限法に基づく上限金利を取得
     *
     * 利息制限法 第1条:
     * - 元本10万円未満: 年20%
     * - 元本10万円以上100万円未満: 年18%
     * - 元本100万円以上: 年15%
     *
     * @param principal 元本
     * @return 上限金利（年率）
     */
    public BigDecimal getLegalMaxRate(BigDecimal principal) {
        if (principal.compareTo(LegalConstants.THRESHOLD_100K) < 0) {
            return LegalConstants.INTEREST_LIMIT_UNDER_100K;
        } else if (principal.compareTo(LegalConstants.THRESHOLD_1M) < 0) {
            return LegalConstants.INTEREST_LIMIT_100K_TO_1M;
        } else {
            return LegalConstants.INTEREST_LIMIT_OVER_1M;
        }
    }

    /**
     * 金利が利息制限法の上限を超えているか確認
     *
     * @param principal 元本
     * @param annualRate 適用金利（年率）
     * @return 超過している場合は true
     */
    public boolean isRateExceedsLimit(BigDecimal principal, BigDecimal annualRate) {
        BigDecimal maxRate = getLegalMaxRate(principal);
        return annualRate.compareTo(maxRate) > 0;
    }

    /**
     * 年利から月利を計算
     *
     * @param annualRate 年利
     * @return 月利
     */
    public BigDecimal getMonthlyRate(BigDecimal annualRate) {
        return annualRate.divide(BigDecimal.valueOf(12), LegalConstants.RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 年利から日利を計算（365日ベース）
     *
     * @param annualRate 年利
     * @return 日利
     */
    public BigDecimal getDailyRate(BigDecimal annualRate) {
        return annualRate.divide(BigDecimal.valueOf(365), LegalConstants.RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 月次利息を計算
     *
     * @param balance 残高
     * @param annualRate 年利
     * @return 月次利息
     */
    public BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal annualRate) {
        BigDecimal monthlyRate = getMonthlyRate(annualRate);
        return balance.multiply(monthlyRate)
                .setScale(LegalConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 日割り利息を計算
     *
     * @param balance 残高
     * @param annualRate 年利
     * @param days 日数
     * @return 日割り利息
     */
    public BigDecimal calculateDailyInterest(BigDecimal balance, BigDecimal annualRate, int days) {
        BigDecimal dailyRate = getDailyRate(annualRate);
        return balance.multiply(dailyRate)
                .multiply(BigDecimal.valueOf(days))
                .setScale(LegalConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 元利均等返済の月々返済額を計算
     *
     * 計算式: PMT = P × r × (1 + r)^n ÷ ((1 + r)^n - 1)
     *
     * @param principal 元本
     * @param annualRate 年利
     * @param termMonths 返済期間（月数）
     * @return 月々返済額
     */
    public BigDecimal calculateEqualTotalPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            // 無利息の場合は単純に等分
            return principal.divide(BigDecimal.valueOf(termMonths), LegalConstants.MONEY_SCALE, RoundingMode.CEILING);
        }

        BigDecimal monthlyRate = getMonthlyRate(annualRate);

        // (1 + r)^n を計算
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(termMonths);

        // 分子: P × r × (1 + r)^n
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowN);

        // 分母: (1 + r)^n - 1
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, LegalConstants.MONEY_SCALE, RoundingMode.CEILING);
    }

    /**
     * 元金均等返済の月々元金返済額を計算
     *
     * @param principal 元本
     * @param termMonths 返済期間（月数）
     * @return 月々の元金返済額
     */
    public BigDecimal calculateEqualPrincipalPayment(BigDecimal principal, int termMonths) {
        return principal.divide(BigDecimal.valueOf(termMonths), LegalConstants.MONEY_SCALE, RoundingMode.CEILING);
    }

    /**
     * 分割払いの手数料を含む総額を計算
     *
     * アドオン方式（多くのクレジットカード会社で採用）:
     * 手数料 = 利用金額 × 手数料率 × 支払回数 ÷ 12
     *
     * @param principal 利用金額
     * @param annualRate 実質年率
     * @param installmentCount 分割回数
     * @return 手数料込み総額
     */
    public BigDecimal calculateInstallmentTotal(BigDecimal principal, BigDecimal annualRate, int installmentCount) {
        // アドオン方式の手数料計算
        BigDecimal fee = principal
                .multiply(annualRate)
                .multiply(BigDecimal.valueOf(installmentCount))
                .divide(BigDecimal.valueOf(12), LegalConstants.MONEY_SCALE, RoundingMode.HALF_UP);

        return principal.add(fee);
    }

    /**
     * 分割払いの月々支払額を計算
     *
     * @param principal 利用金額
     * @param annualRate 実質年率
     * @param installmentCount 分割回数
     * @return 月々支払額
     */
    public BigDecimal calculateInstallmentMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int installmentCount) {
        BigDecimal total = calculateInstallmentTotal(principal, annualRate, installmentCount);
        return total.divide(BigDecimal.valueOf(installmentCount), LegalConstants.MONEY_SCALE, RoundingMode.CEILING);
    }

    /**
     * 遅延損害金を計算
     *
     * 利息制限法第4条:
     * 遅延損害金の上限は法定利息の1.46倍
     *
     * @param principal 元本
     * @param annualRate 適用金利
     * @param delayDays 遅延日数
     * @return 遅延損害金
     */
    public BigDecimal calculateDelayPenalty(BigDecimal principal, BigDecimal annualRate, int delayDays) {
        BigDecimal maxRate = getLegalMaxRate(principal);
        BigDecimal penaltyRate = maxRate.multiply(LegalConstants.DELAY_DAMAGE_MULTIPLIER);

        // 上限は年20%（貸金業者の場合）
        if (penaltyRate.compareTo(LegalConstants.INVESTMENT_LAW_LIMIT) > 0) {
            penaltyRate = LegalConstants.INVESTMENT_LAW_LIMIT;
        }

        return calculateDailyInterest(principal, penaltyRate, delayDays);
    }

    /**
     * 実質年率（APR）を計算
     *
     * リボ払いや分割払いの実質的な金利負担を計算
     *
     * @param principal 元本
     * @param totalPayment 総返済額
     * @param termMonths 返済期間（月数）
     * @return 実質年率
     */
    public BigDecimal calculateEffectiveAnnualRate(BigDecimal principal, BigDecimal totalPayment, int termMonths) {
        BigDecimal totalInterest = totalPayment.subtract(principal);
        BigDecimal years = BigDecimal.valueOf(termMonths).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

        if (years.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 簡易的なAPR計算（実際はもっと複雑なニュートン法等を使用）
        BigDecimal averageBalance = principal.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        return totalInterest
                .divide(averageBalance, 4, RoundingMode.HALF_UP)
                .divide(years, 4, RoundingMode.HALF_UP);
    }
}
