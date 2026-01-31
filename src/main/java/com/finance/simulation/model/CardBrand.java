package com.finance.simulation.model;

/**
 * クレジットカードブランド
 */
public enum CardBrand {

    VISA("Visa", "4"),
    MASTERCARD("Mastercard", "5,2"),
    JCB("JCB", "35"),
    AMEX("American Express", "34,37"),
    DINERS("Diners Club", "36,38,39"),
    DISCOVER("Discover", "6011,65"),
    UNIONPAY("UnionPay", "62"),
    OTHER("その他", "");

    private final String displayName;
    private final String prefixes;

    CardBrand(String displayName, String prefixes) {
        this.displayName = displayName;
        this.prefixes = prefixes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefixes() {
        return prefixes;
    }
}
