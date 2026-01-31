package com.finance.simulation.model;

import java.math.BigDecimal;

/**
 * 残高スライド方式のテーブル種別
 *
 * リボ払いの残高スライド方式では、利用残高に応じて
 * 毎月の最低支払額が変動します。
 *
 * 【一般的な残高スライドテーブル例】
 * 残高 10万円以下    → 月々 5,000円
 * 残高 10万円超〜20万円以下 → 月々 10,000円
 * 残高 20万円超〜50万円以下 → 月々 15,000円
 * 残高 50万円超〜100万円以下 → 月々 20,000円
 * 残高 100万円超 → 月々 30,000円
 */
public enum BalanceSlideType {

    /**
     * 標準タイプ（多くのカード会社で採用）
     */
    STANDARD("標準", new BalanceSlideTable(
            new BigDecimal[]{
                    new BigDecimal("100000"),
                    new BigDecimal("200000"),
                    new BigDecimal("500000"),
                    new BigDecimal("1000000")
            },
            new BigDecimal[]{
                    new BigDecimal("5000"),
                    new BigDecimal("10000"),
                    new BigDecimal("15000"),
                    new BigDecimal("20000"),
                    new BigDecimal("30000")
            }
    )),

    /**
     * ゆとりタイプ（月々の返済額が少なめ）
     */
    EASY("ゆとり", new BalanceSlideTable(
            new BigDecimal[]{
                    new BigDecimal("100000"),
                    new BigDecimal("200000"),
                    new BigDecimal("500000"),
                    new BigDecimal("1000000")
            },
            new BigDecimal[]{
                    new BigDecimal("3000"),
                    new BigDecimal("6000"),
                    new BigDecimal("10000"),
                    new BigDecimal("15000"),
                    new BigDecimal("20000")
            }
    )),

    /**
     * 短期完済タイプ（月々の返済額が多め）
     */
    QUICK("短期完済", new BalanceSlideTable(
            new BigDecimal[]{
                    new BigDecimal("100000"),
                    new BigDecimal("200000"),
                    new BigDecimal("500000"),
                    new BigDecimal("1000000")
            },
            new BigDecimal[]{
                    new BigDecimal("10000"),
                    new BigDecimal("20000"),
                    new BigDecimal("30000"),
                    new BigDecimal("40000"),
                    new BigDecimal("50000")
            }
    ));

    private final String displayName;
    private final BalanceSlideTable table;

    BalanceSlideType(String displayName, BalanceSlideTable table) {
        this.displayName = displayName;
        this.table = table;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BalanceSlideTable getTable() {
        return table;
    }

    /**
     * 残高に応じた月々の返済額を取得
     *
     * @param balance 現在の利用残高
     * @return 月々の最低返済額
     */
    public BigDecimal getMonthlyPayment(BigDecimal balance) {
        return table.getPaymentForBalance(balance);
    }

    /**
     * 残高スライドテーブル
     */
    public static class BalanceSlideTable {
        private final BigDecimal[] thresholds;
        private final BigDecimal[] payments;

        public BalanceSlideTable(BigDecimal[] thresholds, BigDecimal[] payments) {
            if (payments.length != thresholds.length + 1) {
                throw new IllegalArgumentException(
                        "payments配列はthresholds配列より1つ多い必要があります");
            }
            this.thresholds = thresholds;
            this.payments = payments;
        }

        public BigDecimal getPaymentForBalance(BigDecimal balance) {
            for (int i = 0; i < thresholds.length; i++) {
                if (balance.compareTo(thresholds[i]) <= 0) {
                    return payments[i];
                }
            }
            return payments[payments.length - 1];
        }

        public BigDecimal[] getThresholds() {
            return thresholds;
        }

        public BigDecimal[] getPayments() {
            return payments;
        }
    }
}
