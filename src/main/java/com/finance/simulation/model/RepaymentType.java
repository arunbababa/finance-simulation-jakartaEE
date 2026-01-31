package com.finance.simulation.model;

/**
 * 返済方式の種類
 *
 * 【返済方式の説明】
 *
 * 1. 元利均等返済（EQUAL_TOTAL_PAYMENT）
 *    - 毎月の返済額（元金＋利息）が一定
 *    - 住宅ローンで最も一般的
 *    - 初期は利息の割合が高く、後期は元金の割合が高くなる
 *
 * 2. 元金均等返済（EQUAL_PRINCIPAL_PAYMENT）
 *    - 毎月の元金返済額が一定
 *    - 初期の返済額が高く、徐々に減少
 *    - 総支払利息は元利均等より少ない
 *
 * 3. リボルビング払い（REVOLVING）
 *    - 毎月一定額を返済
 *    - 利用残高に関わらず月々の返済額が固定
 *    - 完済まで時間がかかりやすい
 *
 * 4. 分割払い（INSTALLMENT）
 *    - 購入時に回数を決めて分割
 *    - 3回、6回、12回、24回など
 *    - 手数料（実質年率）が適用される
 *
 * 5. 一括払い（LUMP_SUM）
 *    - 利用額を翌月に全額支払い
 *    - 手数料なし
 */
public enum RepaymentType {

    /** 元利均等返済 - 毎月の支払総額が一定 */
    EQUAL_TOTAL_PAYMENT("元利均等返済", "毎月の返済額（元金＋利息）が一定"),

    /** 元金均等返済 - 毎月の元金返済が一定 */
    EQUAL_PRINCIPAL_PAYMENT("元金均等返済", "毎月の元金返済額が一定、利息は残高に応じて減少"),

    /** リボルビング払い - 定額方式 */
    REVOLVING_FIXED_AMOUNT("リボ払い（定額方式）", "毎月一定額を返済"),

    /** リボルビング払い - 残高スライド方式 */
    REVOLVING_BALANCE_SLIDE("リボ払い（残高スライド方式）", "利用残高に応じて返済額が変動"),

    /** リボルビング払い - 定率方式 */
    REVOLVING_FIXED_RATE("リボ払い（定率方式）", "利用残高の一定割合を返済"),

    /** 分割払い */
    INSTALLMENT("分割払い", "指定回数で均等分割して返済"),

    /** 一括払い */
    LUMP_SUM("一括払い", "翌月に全額を一括返済"),

    /** キャッシング - 一括返済 */
    CASHING_LUMP_SUM("キャッシング一括", "翌月に元金＋利息を一括返済"),

    /** キャッシング - リボ払い */
    CASHING_REVOLVING("キャッシングリボ", "キャッシング利用額をリボ払いで返済");

    private final String displayName;
    private final String description;

    RepaymentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
