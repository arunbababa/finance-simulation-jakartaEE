package com.finance.simulation.service;

import com.finance.simulation.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ローン返済シミュレーションサービス
 *
 * 各種返済方式のシミュレーションを実行し、
 * 月次の返済明細と総支払額を計算します。
 */
@ApplicationScoped
public class LoanSimulationService {

    @Inject
    private InterestCalculator interestCalculator;

    // CDI以外での利用のためのコンストラクタ
    public LoanSimulationService() {
        this.interestCalculator = new InterestCalculator();
    }

    public LoanSimulationService(InterestCalculator interestCalculator) {
        this.interestCalculator = interestCalculator;
    }

    /**
     * 返済シミュレーションを実行
     *
     * @param request シミュレーションリクエスト
     * @return シミュレーション結果
     */
    public LoanSimulationResponse simulate(LoanSimulationRequest request) {
        validateRequest(request);

        return switch (request.getRepaymentType()) {
            case EQUAL_TOTAL_PAYMENT -> simulateEqualTotalPayment(request);
            case EQUAL_PRINCIPAL_PAYMENT -> simulateEqualPrincipalPayment(request);
            case REVOLVING_FIXED_AMOUNT -> simulateRevolvingFixedAmount(request);
            case REVOLVING_BALANCE_SLIDE -> simulateRevolvingBalanceSlide(request);
            case REVOLVING_FIXED_RATE -> simulateRevolvingFixedRate(request);
            case INSTALLMENT -> simulateInstallment(request);
            case LUMP_SUM -> simulateLumpSum(request);
            case CASHING_LUMP_SUM -> simulateCashingLumpSum(request);
            case CASHING_REVOLVING -> simulateRevolvingFixedAmount(request);
        };
    }

    /**
     * 元利均等返済のシミュレーション
     *
     * 【計算方法】
     * 毎月の返済額が一定になるように計算
     * PMT = P × r × (1 + r)^n ÷ ((1 + r)^n - 1)
     */
    private LoanSimulationResponse simulateEqualTotalPayment(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();
        int termMonths = request.getTermMonths();

        BigDecimal monthlyPayment = interestCalculator.calculateEqualTotalPayment(principal, annualRate, termMonths);

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal cumulativePayment = BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.now().plusMonths(1);

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interest = interestCalculator.calculateMonthlyInterest(balance, annualRate);
            BigDecimal principalPart;
            BigDecimal payment;

            if (i == termMonths) {
                // 最終回は残りを全て返済
                principalPart = balance;
                payment = balance.add(interest);
            } else {
                principalPart = monthlyPayment.subtract(interest);
                payment = monthlyPayment;
            }

            BigDecimal newBalance = balance.subtract(principalPart);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO;
            }

            totalInterest = totalInterest.add(interest);
            cumulativePayment = cumulativePayment.add(payment);

            details.add(MonthlyPaymentDetail.builder()
                    .paymentNumber(i)
                    .paymentMonth(currentMonth.plusMonths(i - 1).toString())
                    .totalPayment(payment)
                    .principalPayment(principalPart)
                    .interestPayment(interest)
                    .balanceBefore(balance)
                    .balanceAfter(newBalance)
                    .cumulativePayment(cumulativePayment)
                    .cumulativeInterest(totalInterest)
                    .build());

            balance = newBalance;
        }

        return buildResponse(request, details, cumulativePayment, totalInterest, termMonths, monthlyPayment);
    }

    /**
     * 元金均等返済のシミュレーション
     *
     * 【計算方法】
     * 毎月の元金返済額が一定
     * 利息は残高に対して計算されるため、返済額は徐々に減少
     */
    private LoanSimulationResponse simulateEqualPrincipalPayment(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();
        int termMonths = request.getTermMonths();

        BigDecimal monthlyPrincipal = interestCalculator.calculateEqualPrincipalPayment(principal, termMonths);

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal cumulativePayment = BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.now().plusMonths(1);

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interest = interestCalculator.calculateMonthlyInterest(balance, annualRate);
            BigDecimal principalPart;

            if (i == termMonths) {
                principalPart = balance;
            } else {
                principalPart = monthlyPrincipal;
            }

            BigDecimal payment = principalPart.add(interest);
            BigDecimal newBalance = balance.subtract(principalPart);

            totalInterest = totalInterest.add(interest);
            cumulativePayment = cumulativePayment.add(payment);

            details.add(MonthlyPaymentDetail.builder()
                    .paymentNumber(i)
                    .paymentMonth(currentMonth.plusMonths(i - 1).toString())
                    .totalPayment(payment)
                    .principalPayment(principalPart)
                    .interestPayment(interest)
                    .balanceBefore(balance)
                    .balanceAfter(newBalance)
                    .cumulativePayment(cumulativePayment)
                    .cumulativeInterest(totalInterest)
                    .build());

            balance = newBalance;
        }

        BigDecimal firstPayment = details.isEmpty() ? BigDecimal.ZERO : details.get(0).getTotalPayment();
        return buildResponse(request, details, cumulativePayment, totalInterest, termMonths, firstPayment);
    }

    /**
     * リボ払い（定額方式）のシミュレーション
     *
     * 【計算方法】
     * 毎月一定額（ミニマムペイメント）を返済
     * 返済額のうち、まず利息を支払い、残りが元金返済に充当
     */
    private LoanSimulationResponse simulateRevolvingFixedAmount(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();
        BigDecimal monthlyPayment = request.getMonthlyPayment();

        // 月々の最低返済額が利息以下の場合は返済不可能
        BigDecimal firstMonthInterest = interestCalculator.calculateMonthlyInterest(principal, annualRate);
        if (monthlyPayment.compareTo(firstMonthInterest) <= 0) {
            throw new IllegalArgumentException(
                    "月々の返済額が利息額を下回っています。返済が完了しません。最低でも " +
                            firstMonthInterest.add(BigDecimal.ONE) + " 円以上の設定が必要です。");
        }

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal cumulativePayment = BigDecimal.ZERO;
        int months = 0;

        YearMonth currentMonth = YearMonth.now().plusMonths(1);

        // 最大600ヶ月（50年）まで計算
        while (balance.compareTo(BigDecimal.ZERO) > 0 && months < 600) {
            months++;
            BigDecimal interest = interestCalculator.calculateMonthlyInterest(balance, annualRate);
            BigDecimal payment;
            BigDecimal principalPart;

            if (balance.add(interest).compareTo(monthlyPayment) <= 0) {
                // 残高 + 利息が返済額以下なら完済
                payment = balance.add(interest);
                principalPart = balance;
            } else {
                payment = monthlyPayment;
                principalPart = monthlyPayment.subtract(interest);
            }

            BigDecimal newBalance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
            cumulativePayment = cumulativePayment.add(payment);

            details.add(MonthlyPaymentDetail.builder()
                    .paymentNumber(months)
                    .paymentMonth(currentMonth.plusMonths(months - 1).toString())
                    .totalPayment(payment)
                    .principalPayment(principalPart)
                    .interestPayment(interest)
                    .balanceBefore(balance)
                    .balanceAfter(newBalance)
                    .cumulativePayment(cumulativePayment)
                    .cumulativeInterest(totalInterest)
                    .build());

            balance = newBalance;
        }

        return buildResponse(request, details, cumulativePayment, totalInterest, months, monthlyPayment);
    }

    /**
     * リボ払い（残高スライド方式）のシミュレーション
     *
     * 【計算方法】
     * 利用残高に応じて月々の返済額が変動
     * 残高が減ると返済額も減る
     */
    private LoanSimulationResponse simulateRevolvingBalanceSlide(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();
        BalanceSlideType slideType = request.getBalanceSlideType() != null
                ? request.getBalanceSlideType()
                : BalanceSlideType.STANDARD;

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal cumulativePayment = BigDecimal.ZERO;
        int months = 0;

        YearMonth currentMonth = YearMonth.now().plusMonths(1);

        while (balance.compareTo(BigDecimal.ZERO) > 0 && months < 600) {
            months++;
            BigDecimal interest = interestCalculator.calculateMonthlyInterest(balance, annualRate);
            BigDecimal monthlyPayment = slideType.getMonthlyPayment(balance);
            BigDecimal payment;
            BigDecimal principalPart;

            if (balance.add(interest).compareTo(monthlyPayment) <= 0) {
                payment = balance.add(interest);
                principalPart = balance;
            } else {
                payment = monthlyPayment;
                principalPart = monthlyPayment.subtract(interest);

                // 利息が返済額を超える場合
                if (principalPart.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException(
                            "残高スライドの最低返済額が利息を下回っています。");
                }
            }

            BigDecimal newBalance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
            cumulativePayment = cumulativePayment.add(payment);

            details.add(MonthlyPaymentDetail.builder()
                    .paymentNumber(months)
                    .paymentMonth(currentMonth.plusMonths(months - 1).toString())
                    .totalPayment(payment)
                    .principalPayment(principalPart)
                    .interestPayment(interest)
                    .balanceBefore(balance)
                    .balanceAfter(newBalance)
                    .cumulativePayment(cumulativePayment)
                    .cumulativeInterest(totalInterest)
                    .build());

            balance = newBalance;
        }

        BigDecimal firstPayment = details.isEmpty() ? BigDecimal.ZERO : details.get(0).getTotalPayment();
        return buildResponse(request, details, cumulativePayment, totalInterest, months, firstPayment);
    }

    /**
     * リボ払い（定率方式）のシミュレーション
     *
     * 【計算方法】
     * 残高の一定割合を返済
     * 例: 残高の3%を毎月返済
     */
    private LoanSimulationResponse simulateRevolvingFixedRate(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();
        BigDecimal repaymentRate = request.getRepaymentRate();

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal cumulativePayment = BigDecimal.ZERO;
        int months = 0;

        // 最低返済額（多くのカード会社で設定）
        BigDecimal minimumPayment = new BigDecimal("1000");

        YearMonth currentMonth = YearMonth.now().plusMonths(1);

        while (balance.compareTo(BigDecimal.ZERO) > 0 && months < 600) {
            months++;
            BigDecimal interest = interestCalculator.calculateMonthlyInterest(balance, annualRate);

            // 残高 × 返済率
            BigDecimal calculatedPayment = balance.multiply(repaymentRate)
                    .setScale(0, RoundingMode.CEILING);

            // 最低返済額との比較
            BigDecimal monthlyPayment = calculatedPayment.max(minimumPayment);

            BigDecimal payment;
            BigDecimal principalPart;

            if (balance.add(interest).compareTo(monthlyPayment) <= 0) {
                payment = balance.add(interest);
                principalPart = balance;
            } else {
                payment = monthlyPayment;
                principalPart = monthlyPayment.subtract(interest);
            }

            BigDecimal newBalance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
            cumulativePayment = cumulativePayment.add(payment);

            details.add(MonthlyPaymentDetail.builder()
                    .paymentNumber(months)
                    .paymentMonth(currentMonth.plusMonths(months - 1).toString())
                    .totalPayment(payment)
                    .principalPayment(principalPart)
                    .interestPayment(interest)
                    .balanceBefore(balance)
                    .balanceAfter(newBalance)
                    .cumulativePayment(cumulativePayment)
                    .cumulativeInterest(totalInterest)
                    .build());

            balance = newBalance;
        }

        BigDecimal firstPayment = details.isEmpty() ? BigDecimal.ZERO : details.get(0).getTotalPayment();
        return buildResponse(request, details, cumulativePayment, totalInterest, months, firstPayment);
    }

    /**
     * 分割払いのシミュレーション
     *
     * 【計算方法】
     * アドオン方式: 手数料 = 利用金額 × 手数料率 × 回数 ÷ 12
     * 月々の支払額 = (利用金額 + 手数料) ÷ 回数
     */
    private LoanSimulationResponse simulateInstallment(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();
        int installmentCount = request.getInstallmentCount();

        BigDecimal totalAmount = interestCalculator.calculateInstallmentTotal(principal, annualRate, installmentCount);
        BigDecimal totalFee = totalAmount.subtract(principal);
        BigDecimal monthlyPayment = interestCalculator.calculateInstallmentMonthlyPayment(principal, annualRate, installmentCount);

        // 分割払いの場合、手数料を月数で均等に配分
        BigDecimal monthlyFee = totalFee.divide(BigDecimal.valueOf(installmentCount), 0, RoundingMode.HALF_UP);
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(installmentCount), 0, RoundingMode.HALF_UP);

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal cumulativePayment = BigDecimal.ZERO;
        BigDecimal cumulativeInterest = BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.now().plusMonths(1);

        for (int i = 1; i <= installmentCount; i++) {
            BigDecimal principalPart;
            BigDecimal payment;
            BigDecimal fee;

            if (i == installmentCount) {
                // 最終回は調整
                principalPart = balance;
                fee = totalFee.subtract(cumulativeInterest);
                payment = principalPart.add(fee);
            } else {
                principalPart = monthlyPrincipal;
                fee = monthlyFee;
                payment = monthlyPayment;
            }

            BigDecimal newBalance = balance.subtract(principalPart);
            cumulativePayment = cumulativePayment.add(payment);
            cumulativeInterest = cumulativeInterest.add(fee);

            details.add(MonthlyPaymentDetail.builder()
                    .paymentNumber(i)
                    .paymentMonth(currentMonth.plusMonths(i - 1).toString())
                    .totalPayment(payment)
                    .principalPayment(principalPart)
                    .interestPayment(fee)
                    .balanceBefore(balance)
                    .balanceAfter(newBalance)
                    .cumulativePayment(cumulativePayment)
                    .cumulativeInterest(cumulativeInterest)
                    .build());

            balance = newBalance;
        }

        return buildResponse(request, details, totalAmount, totalFee, installmentCount, monthlyPayment);
    }

    /**
     * 一括払いのシミュレーション
     *
     * 【計算方法】
     * 翌月に全額を一括返済、手数料なし
     */
    private LoanSimulationResponse simulateLumpSum(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        details.add(MonthlyPaymentDetail.builder()
                .paymentNumber(1)
                .paymentMonth(nextMonth.toString())
                .totalPayment(principal)
                .principalPayment(principal)
                .interestPayment(BigDecimal.ZERO)
                .balanceBefore(principal)
                .balanceAfter(BigDecimal.ZERO)
                .cumulativePayment(principal)
                .cumulativeInterest(BigDecimal.ZERO)
                .build());

        return buildResponse(request, details, principal, BigDecimal.ZERO, 1, principal);
    }

    /**
     * キャッシング一括返済のシミュレーション
     *
     * 【計算方法】
     * 利用日から支払日までの日割り利息を計算
     * 翌月に元金 + 利息を一括返済
     */
    private LoanSimulationResponse simulateCashingLumpSum(LoanSimulationRequest request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();

        // 平均的な日数（締め日から支払日までを想定）
        int days = 25;
        BigDecimal interest = interestCalculator.calculateDailyInterest(principal, annualRate, days);
        BigDecimal totalPayment = principal.add(interest);

        List<MonthlyPaymentDetail> details = new ArrayList<>();
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        details.add(MonthlyPaymentDetail.builder()
                .paymentNumber(1)
                .paymentMonth(nextMonth.toString())
                .totalPayment(totalPayment)
                .principalPayment(principal)
                .interestPayment(interest)
                .balanceBefore(principal)
                .balanceAfter(BigDecimal.ZERO)
                .cumulativePayment(totalPayment)
                .cumulativeInterest(interest)
                .build());

        return buildResponse(request, details, totalPayment, interest, 1, totalPayment);
    }

    /**
     * レスポンスを構築
     */
    private LoanSimulationResponse buildResponse(
            LoanSimulationRequest request,
            List<MonthlyPaymentDetail> details,
            BigDecimal totalPayment,
            BigDecimal totalInterest,
            int totalMonths,
            BigDecimal monthlyPayment) {

        BigDecimal principal = request.getPrincipal();
        BigDecimal annualRate = request.getAnnualRate();

        // 法的情報
        BigDecimal legalMaxRate = interestCalculator.getLegalMaxRate(principal);
        String rateWarning = null;
        if (interestCalculator.isRateExceedsLimit(principal, annualRate)) {
            rateWarning = "警告: 適用金利が利息制限法の上限（" +
                    legalMaxRate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) +
                    "%）を超えています。超過分は無効となります。";
        }

        // 実質年率の計算
        BigDecimal effectiveRate = interestCalculator.calculateEffectiveAnnualRate(
                principal, totalPayment, totalMonths);

        List<String> applicableLaws = Arrays.asList(
                "利息制限法（昭和29年法律第100号）",
                "出資の受入れ、預り金及び金利等の取締りに関する法律",
                "貸金業法（昭和58年法律第32号）",
                "割賦販売法（昭和36年法律第159号）"
        );

        BigDecimal firstPayment = details.isEmpty() ? BigDecimal.ZERO : details.get(0).getTotalPayment();
        BigDecimal lastPayment = details.isEmpty() ? BigDecimal.ZERO : details.get(details.size() - 1).getTotalPayment();

        return LoanSimulationResponse.builder()
                .principal(principal)
                .annualRate(annualRate)
                .repaymentType(request.getRepaymentType())
                .totalPayment(totalPayment)
                .totalInterest(totalInterest)
                .totalMonths(totalMonths)
                .monthlyPayment(monthlyPayment)
                .firstPayment(firstPayment)
                .lastPayment(lastPayment)
                .effectiveAnnualRate(effectiveRate)
                .legalMaxRate(legalMaxRate)
                .rateWarning(rateWarning)
                .applicableLaws(applicableLaws)
                .monthlyDetails(details)
                .build();
    }

    /**
     * リクエストのバリデーション
     */
    private void validateRequest(LoanSimulationRequest request) {
        if (request.getPrincipal() == null || request.getPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("借入金額は0より大きい必要があります");
        }

        if (request.getAnnualRate() == null || request.getAnnualRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金利は0以上である必要があります");
        }

        if (request.getRepaymentType() == null) {
            throw new IllegalArgumentException("返済方式は必須です");
        }

        // 返済方式ごとの必須パラメータチェック
        switch (request.getRepaymentType()) {
            case EQUAL_TOTAL_PAYMENT, EQUAL_PRINCIPAL_PAYMENT:
                if (request.getTermMonths() == null || request.getTermMonths() <= 0) {
                    throw new IllegalArgumentException("返済期間は1ヶ月以上である必要があります");
                }
                break;
            case REVOLVING_FIXED_AMOUNT, CASHING_REVOLVING:
                if (request.getMonthlyPayment() == null || request.getMonthlyPayment().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("月々の返済額は0より大きい必要があります");
                }
                break;
            case REVOLVING_FIXED_RATE:
                if (request.getRepaymentRate() == null || request.getRepaymentRate().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("返済率は0より大きい必要があります");
                }
                break;
            case INSTALLMENT:
                if (request.getInstallmentCount() == null || request.getInstallmentCount() <= 1) {
                    throw new IllegalArgumentException("分割回数は2回以上である必要があります");
                }
                break;
            default:
                break;
        }
    }
}
