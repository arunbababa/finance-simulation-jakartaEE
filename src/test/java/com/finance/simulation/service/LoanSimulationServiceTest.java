package com.finance.simulation.service;

import com.finance.simulation.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ローン返済シミュレーションサービスのテスト
 */
class LoanSimulationServiceTest {

    private LoanSimulationService service;

    @BeforeEach
    void setUp() {
        service = new LoanSimulationService();
    }

    @Nested
    @DisplayName("元利均等返済のテスト")
    class EqualTotalPaymentTest {

        @Test
        @DisplayName("正常にシミュレーションが実行される")
        void shouldSimulateSuccessfully() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("300000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                    .termMonths(24)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response);
            assertEquals(24, response.getTotalMonths());
            assertEquals(24, response.getMonthlyDetails().size());

            // 総返済額は元本より大きい
            assertTrue(response.getTotalPayment().compareTo(request.getPrincipal()) > 0);

            // 最終残高は0
            BigDecimal finalBalance = response.getMonthlyDetails().get(23).getBalanceAfter();
            assertEquals(0, finalBalance.compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("月々の返済額が一定である")
        void shouldHaveConstantMonthlyPayment() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("500000"))
                    .annualRate(new BigDecimal("0.12"))
                    .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                    .termMonths(12)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            // 最終回以外は同じ返済額
            BigDecimal firstPayment = response.getMonthlyDetails().get(0).getTotalPayment();
            for (int i = 1; i < response.getMonthlyDetails().size() - 1; i++) {
                assertEquals(firstPayment, response.getMonthlyDetails().get(i).getTotalPayment());
            }
        }
    }

    @Nested
    @DisplayName("元金均等返済のテスト")
    class EqualPrincipalPaymentTest {

        @Test
        @DisplayName("正常にシミュレーションが実行される")
        void shouldSimulateSuccessfully() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("240000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.EQUAL_PRINCIPAL_PAYMENT)
                    .termMonths(12)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response);
            assertEquals(12, response.getTotalMonths());

            // 元金返済分が一定（最終回以外）
            BigDecimal monthlyPrincipal = new BigDecimal("20000"); // 240000 / 12
            for (int i = 0; i < response.getMonthlyDetails().size() - 1; i++) {
                assertEquals(monthlyPrincipal, response.getMonthlyDetails().get(i).getPrincipalPayment());
            }
        }

        @Test
        @DisplayName("返済額は徐々に減少する")
        void shouldDecreaseMonthlyPayment() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("1000000"))
                    .annualRate(new BigDecimal("0.18"))
                    .repaymentType(RepaymentType.EQUAL_PRINCIPAL_PAYMENT)
                    .termMonths(24)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            // 初回 > 最終回（利息が減るため）
            BigDecimal firstPayment = response.getMonthlyDetails().get(0).getTotalPayment();
            BigDecimal lastPayment = response.getMonthlyDetails().get(23).getTotalPayment();
            assertTrue(firstPayment.compareTo(lastPayment) > 0);
        }
    }

    @Nested
    @DisplayName("リボ払い（定額方式）のテスト")
    class RevolvingFixedAmountTest {

        @Test
        @DisplayName("正常にシミュレーションが実行される")
        void shouldSimulateSuccessfully() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.REVOLVING_FIXED_AMOUNT)
                    .monthlyPayment(new BigDecimal("10000"))
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response);
            assertTrue(response.getTotalMonths() > 0);

            // 最終残高は0
            int lastIndex = response.getMonthlyDetails().size() - 1;
            BigDecimal finalBalance = response.getMonthlyDetails().get(lastIndex).getBalanceAfter();
            assertEquals(0, finalBalance.compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("月々返済額が利息以下の場合はエラー")
        void shouldThrowExceptionWhenPaymentTooLow() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("1000000"))
                    .annualRate(new BigDecimal("0.18"))
                    .repaymentType(RepaymentType.REVOLVING_FIXED_AMOUNT)
                    .monthlyPayment(new BigDecimal("5000")) // 利息だけで15000円程度
                    .build();

            assertThrows(IllegalArgumentException.class, () -> service.simulate(request));
        }
    }

    @Nested
    @DisplayName("リボ払い（残高スライド方式）のテスト")
    class RevolvingBalanceSlideTest {

        @Test
        @DisplayName("正常にシミュレーションが実行される")
        void shouldSimulateSuccessfully() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("200000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.REVOLVING_BALANCE_SLIDE)
                    .balanceSlideType(BalanceSlideType.STANDARD)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response);
            assertTrue(response.getTotalMonths() > 0);
        }

        @Test
        @DisplayName("残高が減ると返済額も変化する")
        void shouldAdjustPaymentBasedOnBalance() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("300000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.REVOLVING_BALANCE_SLIDE)
                    .balanceSlideType(BalanceSlideType.STANDARD)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            // 残高が10万円を下回ると返済額が変わるはず
            boolean foundTransition = false;
            for (int i = 1; i < response.getMonthlyDetails().size(); i++) {
                MonthlyPaymentDetail prev = response.getMonthlyDetails().get(i - 1);
                MonthlyPaymentDetail curr = response.getMonthlyDetails().get(i);
                if (!prev.getTotalPayment().equals(curr.getTotalPayment())) {
                    foundTransition = true;
                    break;
                }
            }
            assertTrue(foundTransition);
        }
    }

    @Nested
    @DisplayName("分割払いのテスト")
    class InstallmentTest {

        @Test
        @DisplayName("正常にシミュレーションが実行される")
        void shouldSimulateSuccessfully() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualRate(new BigDecimal("0.12"))
                    .repaymentType(RepaymentType.INSTALLMENT)
                    .installmentCount(12)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response);
            assertEquals(12, response.getTotalMonths());
            assertEquals(12, response.getMonthlyDetails().size());
        }

        @Test
        @DisplayName("各回の支払額がほぼ均等")
        void shouldHaveNearlyEqualPayments() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("120000"))
                    .annualRate(new BigDecimal("0.12"))
                    .repaymentType(RepaymentType.INSTALLMENT)
                    .installmentCount(6)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            BigDecimal firstPayment = response.getMonthlyDetails().get(0).getTotalPayment();
            for (MonthlyPaymentDetail detail : response.getMonthlyDetails()) {
                // 端数調整で多少の差はあり得る
                BigDecimal diff = firstPayment.subtract(detail.getTotalPayment()).abs();
                assertTrue(diff.compareTo(new BigDecimal("100")) < 0);
            }
        }
    }

    @Nested
    @DisplayName("一括払いのテスト")
    class LumpSumTest {

        @Test
        @DisplayName("翌月に全額返済、手数料なし")
        void shouldPayFullAmountWithNoInterest() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("50000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.LUMP_SUM)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response);
            assertEquals(1, response.getTotalMonths());
            assertEquals(new BigDecimal("50000"), response.getTotalPayment());
            assertEquals(BigDecimal.ZERO, response.getTotalInterest());
        }
    }

    @Nested
    @DisplayName("バリデーションのテスト")
    class ValidationTest {

        @Test
        @DisplayName("借入金額が0以下の場合はエラー")
        void shouldThrowExceptionWhenPrincipalIsZero() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(BigDecimal.ZERO)
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                    .termMonths(12)
                    .build();

            assertThrows(IllegalArgumentException.class, () -> service.simulate(request));
        }

        @Test
        @DisplayName("返済方式がnullの場合はエラー")
        void shouldThrowExceptionWhenRepaymentTypeIsNull() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(null)
                    .build();

            assertThrows(IllegalArgumentException.class, () -> service.simulate(request));
        }

        @Test
        @DisplayName("元利均等返済で返済期間がない場合はエラー")
        void shouldThrowExceptionWhenTermMonthsIsMissing() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                    .termMonths(null)
                    .build();

            assertThrows(IllegalArgumentException.class, () -> service.simulate(request));
        }
    }

    @Nested
    @DisplayName("法的情報のテスト")
    class LegalInfoTest {

        @Test
        @DisplayName("レスポンスに法的情報が含まれる")
        void shouldIncludeLegalInfo() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("300000"))
                    .annualRate(new BigDecimal("0.15"))
                    .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                    .termMonths(12)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response.getLegalMaxRate());
            assertNotNull(response.getApplicableLaws());
            assertFalse(response.getApplicableLaws().isEmpty());
        }

        @Test
        @DisplayName("上限金利超過時に警告が表示される")
        void shouldShowWarningWhenRateExceedsLimit() {
            LoanSimulationRequest request = LoanSimulationRequest.builder()
                    .principal(new BigDecimal("300000"))
                    .annualRate(new BigDecimal("0.19")) // 18%を超過
                    .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                    .termMonths(12)
                    .build();

            LoanSimulationResponse response = service.simulate(request);

            assertNotNull(response.getRateWarning());
            assertTrue(response.getRateWarning().contains("警告"));
        }
    }
}
