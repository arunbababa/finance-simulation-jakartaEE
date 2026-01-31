package com.finance.simulation.model;

import java.math.BigDecimal;

/**
 * 金融関連法規に基づく定数定義
 *
 * 【法的根拠】
 *
 * 1. 利息制限法（昭和29年法律第100号）第1条
 *    - 元本10万円未満: 年20%
 *    - 元本10万円以上100万円未満: 年18%
 *    - 元本100万円以上: 年15%
 *    参照: https://elaws.e-gov.go.jp/document?lawid=329AC0000000100
 *
 * 2. 出資の受入れ、預り金及び金利等の取締りに関する法律（出資法）第5条
 *    - 貸金業者の上限金利: 年20%
 *    参照: https://elaws.e-gov.go.jp/document?lawid=329AC0000000195
 *
 * 3. 貸金業法 第13条の2（総量規制）
 *    - 個人向け貸付: 年収の1/3まで
 *    参照: https://elaws.e-gov.go.jp/document?lawid=358AC0000000032
 *
 * 4. 割賦販売法
 *    - クレジットカードの分割払い・リボ払いに関する規制
 *    参照: https://elaws.e-gov.go.jp/document?lawid=336AC0000000159
 */
public final class LegalConstants {

    private LegalConstants() {
        // ユーティリティクラスのためインスタンス化禁止
    }

    // ========================================
    // 利息制限法に基づく上限金利（年率）
    // ========================================

    /** 元本10万円未満の上限金利: 年20% */
    public static final BigDecimal INTEREST_LIMIT_UNDER_100K = new BigDecimal("0.20");

    /** 元本10万円以上100万円未満の上限金利: 年18% */
    public static final BigDecimal INTEREST_LIMIT_100K_TO_1M = new BigDecimal("0.18");

    /** 元本100万円以上の上限金利: 年15% */
    public static final BigDecimal INTEREST_LIMIT_OVER_1M = new BigDecimal("0.15");

    /** 利息制限法の元本区分: 10万円 */
    public static final BigDecimal THRESHOLD_100K = new BigDecimal("100000");

    /** 利息制限法の元本区分: 100万円 */
    public static final BigDecimal THRESHOLD_1M = new BigDecimal("1000000");

    // ========================================
    // 出資法に基づく上限金利
    // ========================================

    /** 出資法上限金利（貸金業者）: 年20% */
    public static final BigDecimal INVESTMENT_LAW_LIMIT = new BigDecimal("0.20");

    // ========================================
    // 貸金業法に基づく総量規制
    // ========================================

    /** 総量規制の比率: 年収の1/3 */
    public static final BigDecimal TOTAL_VOLUME_REGULATION_RATIO = new BigDecimal("0.333333");

    // ========================================
    // 遅延損害金の上限（利息制限法第4条）
    // ========================================

    /** 遅延損害金上限: 法定利息の1.46倍 */
    public static final BigDecimal DELAY_DAMAGE_MULTIPLIER = new BigDecimal("1.46");

    // ========================================
    // 一般的なクレジットカード金利（参考値）
    // ========================================

    /** 一般的なリボ払い金利: 年15% */
    public static final BigDecimal TYPICAL_REVOLVING_RATE = new BigDecimal("0.15");

    /** 一般的なキャッシング金利: 年18% */
    public static final BigDecimal TYPICAL_CASHING_RATE = new BigDecimal("0.18");

    /** 分割払い手数料率（実質年率）: 年12-15%程度 */
    public static final BigDecimal TYPICAL_INSTALLMENT_RATE = new BigDecimal("0.12");

    // ========================================
    // 計算精度
    // ========================================

    /** 金額計算の小数点以下桁数 */
    public static final int MONEY_SCALE = 0;

    /** 金利計算の小数点以下桁数 */
    public static final int RATE_SCALE = 10;
}
