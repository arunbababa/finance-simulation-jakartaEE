package com.finance.simulation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 利息計算サービスのテスト
 */
class InterestCalculatorTest {

    private InterestCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new InterestCalculator();
    }

    @Nested
    @DisplayName("利息制限法に基づく上限金利のテスト")
    class LegalMaxRateTest {

        @Test
        @DisplayName("元本10万円未満の場合、上限金利は20%")
        void shouldReturn20PercentForUnder100K() {
            BigDecimal principal = new BigDecimal("50000");
            BigDecimal maxRate = calculator.getLegalMaxRate(principal);
            assertEquals(new BigDecimal("0.20"), maxRate);
        }

        @Test
        @DisplayName("元本10万円の場合、上限金利は18%")
        void shouldReturn18PercentFor100K() {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal maxRate = calculator.getLegalMaxRate(principal);
            assertEquals(new BigDecimal("0.18"), maxRate);
        }

        @Test
        @DisplayName("元本50万円の場合、上限金利は18%")
        void shouldReturn18PercentFor500K() {
            BigDecimal principal = new BigDecimal("500000");
            BigDecimal maxRate = calculator.getLegalMaxRate(principal);
            assertEquals(new BigDecimal("0.18"), maxRate);
        }

        @Test
        @DisplayName("元本100万円の場合、上限金利は15%")
        void shouldReturn15PercentFor1M() {
            BigDecimal principal = new BigDecimal("1000000");
            BigDecimal maxRate = calculator.getLegalMaxRate(principal);
            assertEquals(new BigDecimal("0.15"), maxRate);
        }

        @Test
        @DisplayName("元本500万円の場合、上限金利は15%")
        void shouldReturn15PercentFor5M() {
            BigDecimal principal = new BigDecimal("5000000");
            BigDecimal maxRate = calculator.getLegalMaxRate(principal);
            assertEquals(new BigDecimal("0.15"), maxRate);
        }
    }

    @Nested
    @DisplayName("金利超過チェックのテスト")
    class RateExceedsLimitTest {

        @Test
        @DisplayName("上限以下の金利は超過と判定されない")
        void shouldNotExceedWhenWithinLimit() {
            BigDecimal principal = new BigDecimal("300000");
            BigDecimal rate = new BigDecimal("0.15");
            assertFalse(calculator.isRateExceedsLimit(principal, rate));
        }

        @Test
        @DisplayName("上限を超える金利は超過と判定される")
        void shouldExceedWhenOverLimit() {
            BigDecimal principal = new BigDecimal("300000");
            BigDecimal rate = new BigDecimal("0.19"); // 18%を超過
            assertTrue(calculator.isRateExceedsLimit(principal, rate));
        }

        @Test
        @DisplayName("上限と同じ金利は超過と判定されない")
        void shouldNotExceedWhenEqualToLimit() {
            BigDecimal principal = new BigDecimal("300000");
            BigDecimal rate = new BigDecimal("0.18"); // ちょうど18%
            assertFalse(calculator.isRateExceedsLimit(principal, rate));
        }
    }

    @Nested
    @DisplayName("月利計算のテスト")
    class MonthlyRateTest {

        @Test
        @DisplayName("年利15%の月利は約1.25%")
        void shouldCalculateMonthlyRate() {
            BigDecimal annualRate = new BigDecimal("0.15");
            BigDecimal monthlyRate = calculator.getMonthlyRate(annualRate);
            assertEquals(0, monthlyRate.compareTo(new BigDecimal("0.0125")));
        }
    }

    @Nested
    @DisplayName("元利均等返済のテスト")
    class EqualTotalPaymentTest {

        @Test
        @DisplayName("100万円を年利15%で12ヶ月返済の場合の月額")
        void shouldCalculateMonthlyPayment() {
            BigDecimal principal = new BigDecimal("1000000");
            BigDecimal annualRate = new BigDecimal("0.15");
            int termMonths = 12;

            BigDecimal monthlyPayment = calculator.calculateEqualTotalPayment(principal, annualRate, termMonths);

            // 概算で月額約90,000円前後
            assertTrue(monthlyPayment.compareTo(new BigDecimal("85000")) > 0);
            assertTrue(monthlyPayment.compareTo(new BigDecimal("95000")) < 0);
        }

        @Test
        @DisplayName("無利息の場合は単純に等分")
        void shouldDivideEquallyWhenNoInterest() {
            BigDecimal principal = new BigDecimal("120000");
            BigDecimal annualRate = BigDecimal.ZERO;
            int termMonths = 12;

            BigDecimal monthlyPayment = calculator.calculateEqualTotalPayment(principal, annualRate, termMonths);

            assertEquals(new BigDecimal("10000"), monthlyPayment);
        }
    }

    @Nested
    @DisplayName("分割払い計算のテスト")
    class InstallmentTest {

        @Test
        @DisplayName("10万円を年利12%で12回分割の総額")
        void shouldCalculateInstallmentTotal() {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.12");
            int installmentCount = 12;

            BigDecimal total = calculator.calculateInstallmentTotal(principal, annualRate, installmentCount);

            // 手数料 = 100000 × 0.12 × 12 ÷ 12 = 12000
            // 総額 = 100000 + 12000 = 112000
            assertEquals(new BigDecimal("112000"), total);
        }

        @Test
        @DisplayName("分割払いの月額計算")
        void shouldCalculateInstallmentMonthlyPayment() {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.12");
            int installmentCount = 10;

            BigDecimal monthlyPayment = calculator.calculateInstallmentMonthlyPayment(principal, annualRate, installmentCount);

            // 総額 = 100000 + (100000 × 0.12 × 10 ÷ 12) = 110000
            // 月額 = 110000 ÷ 10 = 11000
            assertEquals(new BigDecimal("11000"), monthlyPayment);
        }
    }

    @Nested
    @DisplayName("月次利息計算のテスト")
    class MonthlyInterestTest {

        @Test
        @DisplayName("残高30万円、年利15%の月次利息")
        void shouldCalculateMonthlyInterest() {
            BigDecimal balance = new BigDecimal("300000");
            BigDecimal annualRate = new BigDecimal("0.15");

            BigDecimal interest = calculator.calculateMonthlyInterest(balance, annualRate);

            // 300000 × 0.15 ÷ 12 = 3750
            assertEquals(new BigDecimal("3750"), interest);
        }
    }

    @Nested
    @DisplayName("日割り利息計算のテスト")
    class DailyInterestTest {

        @Test
        @DisplayName("残高10万円、年利18%、30日間の日割り利息")
        void shouldCalculateDailyInterest() {
            BigDecimal balance = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.18");
            int days = 30;

            BigDecimal interest = calculator.calculateDailyInterest(balance, annualRate, days);

            // 100000 × (0.18 ÷ 365) × 30 ≈ 1479
            assertTrue(interest.compareTo(new BigDecimal("1400")) > 0);
            assertTrue(interest.compareTo(new BigDecimal("1500")) < 0);
        }
    }
}
