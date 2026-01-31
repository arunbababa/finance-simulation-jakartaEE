package com.finance.simulation.model;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * クレジットカード情報エンティティ
 *
 * 【セキュリティ上の注意事項】
 *
 * 1. PCI DSS（Payment Card Industry Data Security Standard）準拠
 *    - カード番号は暗号化して保存
 *    - CVV/CVC（セキュリティコード）は保存禁止
 *    - アクセスログの記録
 *
 * 2. カード番号の扱い
 *    - 表示時はマスキング（例: **** **** **** 1234）
 *    - データベースには暗号化して保存
 *    - 本番環境では HSM（Hardware Security Module）を使用
 *
 * 3. 割賦販売法に基づく義務
 *    - 加盟店はカード情報の非保持化が推奨
 *    - トークン化の利用を検討
 *
 * 【このサンプルについて】
 * これは学習用のサンプルコードです。
 * 本番環境では、決済代行サービス（Stripe, PAY.JP等）の
 * トークン化機能を使用してください。
 */
@Entity
@Table(name = "card_info")
public class CardInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** カード名義人 */
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "card_holder_name", nullable = false)
    @JsonbProperty("cardHolderName")
    private String cardHolderName;

    /**
     * カード番号（暗号化済み）
     *
     * 【実装上の注意】
     * - 本番環境では AES-256 等で暗号化
     * - このサンプルでは簡易的に保存
     */
    @NotNull
    @Column(name = "card_number_encrypted", nullable = false)
    @JsonbTransient  // JSON出力時は除外
    private String cardNumberEncrypted;

    /** カード番号下4桁（表示用） */
    @Column(name = "card_last_four")
    @JsonbProperty("cardLastFour")
    private String cardLastFour;

    /** カードブランド */
    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand")
    @JsonbProperty("cardBrand")
    private CardBrand cardBrand;

    /** 有効期限（年月） */
    @NotNull
    @Column(name = "expiry_date", nullable = false)
    @JsonbProperty("expiryDate")
    private YearMonth expiryDate;

    /**
     * CVV/CVC（セキュリティコード）
     *
     * 【重要】PCI DSSでは保存禁止
     * 決済時の一時的な認証にのみ使用し、
     * データベースには絶対に保存しない
     */
    @Transient  // DBに保存しない
    @JsonbTransient  // JSON出力時も除外
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVVは3〜4桁の数字です")
    private String cvv;

    // ========================================
    // クレジット利用に関する情報
    // ========================================

    /** ショッピング利用枠 */
    @Column(name = "shopping_limit")
    @JsonbProperty("shoppingLimit")
    private BigDecimal shoppingLimit;

    /** キャッシング利用枠 */
    @Column(name = "cashing_limit")
    @JsonbProperty("cashingLimit")
    private BigDecimal cashingLimit;

    /** リボ払い利用枠 */
    @Column(name = "revolving_limit")
    @JsonbProperty("revolvingLimit")
    private BigDecimal revolvingLimit;

    /** 現在のショッピング利用残高 */
    @Column(name = "shopping_balance")
    @JsonbProperty("shoppingBalance")
    private BigDecimal shoppingBalance;

    /** 現在のキャッシング残高 */
    @Column(name = "cashing_balance")
    @JsonbProperty("cashingBalance")
    private BigDecimal cashingBalance;

    /** 現在のリボ残高 */
    @Column(name = "revolving_balance")
    @JsonbProperty("revolvingBalance")
    private BigDecimal revolvingBalance;

    /** ショッピングリボ金利（年率） */
    @Column(name = "shopping_revolving_rate")
    @JsonbProperty("shoppingRevolvingRate")
    private BigDecimal shoppingRevolvingRate;

    /** キャッシング金利（年率） */
    @Column(name = "cashing_rate")
    @JsonbProperty("cashingRate")
    private BigDecimal cashingRate;

    /** 支払日（毎月何日か） */
    @Column(name = "payment_day")
    @JsonbProperty("paymentDay")
    private Integer paymentDay;

    /** 締め日（毎月何日か） */
    @Column(name = "closing_day")
    @JsonbProperty("closingDay")
    private Integer closingDay;

    /** カード発行日 */
    @Column(name = "issue_date")
    @JsonbProperty("issueDate")
    private LocalDate issueDate;

    // ========================================
    // コンストラクタ
    // ========================================

    public CardInfo() {
        this.shoppingBalance = BigDecimal.ZERO;
        this.cashingBalance = BigDecimal.ZERO;
        this.revolvingBalance = BigDecimal.ZERO;
    }

    // ========================================
    // ビジネスロジック
    // ========================================

    /**
     * カード番号を設定（暗号化処理を含む）
     *
     * @param cardNumber 平文のカード番号
     */
    public void setCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            throw new IllegalArgumentException("無効なカード番号です");
        }

        // カード番号のバリデーション（Luhnアルゴリズム）
        if (!validateCardNumber(cardNumber)) {
            throw new IllegalArgumentException("カード番号が不正です（チェックサムエラー）");
        }

        // 下4桁を保存
        this.cardLastFour = cardNumber.substring(cardNumber.length() - 4);

        // カードブランドを判定
        this.cardBrand = detectCardBrand(cardNumber);

        // 暗号化して保存（本番では適切な暗号化を使用）
        // このサンプルでは簡易的に保存
        this.cardNumberEncrypted = encryptCardNumber(cardNumber);
    }

    /**
     * マスキングされたカード番号を取得
     *
     * @return マスキングされたカード番号（例: **** **** **** 1234）
     */
    @JsonbProperty("maskedCardNumber")
    public String getMaskedCardNumber() {
        if (cardLastFour == null) {
            return null;
        }
        return "**** **** **** " + cardLastFour;
    }

    /**
     * カードが有効期限内かどうかを確認
     *
     * @return 有効期限内であれば true
     */
    public boolean isValid() {
        if (expiryDate == null) {
            return false;
        }
        return !YearMonth.now().isAfter(expiryDate);
    }

    /**
     * ショッピング利用可能額を取得
     *
     * @return 利用可能額
     */
    public BigDecimal getAvailableShoppingLimit() {
        if (shoppingLimit == null || shoppingBalance == null) {
            return BigDecimal.ZERO;
        }
        return shoppingLimit.subtract(shoppingBalance);
    }

    /**
     * キャッシング利用可能額を取得
     *
     * @return 利用可能額
     */
    public BigDecimal getAvailableCashingLimit() {
        if (cashingLimit == null || cashingBalance == null) {
            return BigDecimal.ZERO;
        }
        return cashingLimit.subtract(cashingBalance);
    }

    // ========================================
    // プライベートメソッド
    // ========================================

    /**
     * Luhnアルゴリズムによるカード番号の検証
     *
     * @param cardNumber カード番号
     * @return 有効であれば true
     */
    private boolean validateCardNumber(String cardNumber) {
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * カード番号からブランドを判定
     *
     * @param cardNumber カード番号
     * @return カードブランド
     */
    private CardBrand detectCardBrand(String cardNumber) {
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("4")) {
            return CardBrand.VISA;
        } else if (digits.startsWith("5") || digits.startsWith("2")) {
            return CardBrand.MASTERCARD;
        } else if (digits.startsWith("35")) {
            return CardBrand.JCB;
        } else if (digits.startsWith("34") || digits.startsWith("37")) {
            return CardBrand.AMEX;
        } else if (digits.startsWith("36") || digits.startsWith("38") || digits.startsWith("39")) {
            return CardBrand.DINERS;
        }
        return CardBrand.OTHER;
    }

    /**
     * カード番号を暗号化
     *
     * 【注意】これはサンプル実装です
     * 本番環境では AES-256-GCM 等の適切な暗号化を使用してください
     *
     * @param cardNumber 平文のカード番号
     * @return 暗号化されたカード番号
     */
    private String encryptCardNumber(String cardNumber) {
        // TODO: 本番環境では適切な暗号化を実装
        // 例: AES-256-GCM with HSM
        // このサンプルでは Base64 エンコードのみ（非推奨）
        return java.util.Base64.getEncoder()
                .encodeToString(cardNumber.getBytes());
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public CardBrand getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(CardBrand cardBrand) {
        this.cardBrand = cardBrand;
    }

    public YearMonth getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(YearMonth expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getShoppingLimit() {
        return shoppingLimit;
    }

    public void setShoppingLimit(BigDecimal shoppingLimit) {
        this.shoppingLimit = shoppingLimit;
    }

    public BigDecimal getCashingLimit() {
        return cashingLimit;
    }

    public void setCashingLimit(BigDecimal cashingLimit) {
        this.cashingLimit = cashingLimit;
    }

    public BigDecimal getRevolvingLimit() {
        return revolvingLimit;
    }

    public void setRevolvingLimit(BigDecimal revolvingLimit) {
        this.revolvingLimit = revolvingLimit;
    }

    public BigDecimal getShoppingBalance() {
        return shoppingBalance;
    }

    public void setShoppingBalance(BigDecimal shoppingBalance) {
        this.shoppingBalance = shoppingBalance;
    }

    public BigDecimal getCashingBalance() {
        return cashingBalance;
    }

    public void setCashingBalance(BigDecimal cashingBalance) {
        this.cashingBalance = cashingBalance;
    }

    public BigDecimal getRevolvingBalance() {
        return revolvingBalance;
    }

    public void setRevolvingBalance(BigDecimal revolvingBalance) {
        this.revolvingBalance = revolvingBalance;
    }

    public BigDecimal getShoppingRevolvingRate() {
        return shoppingRevolvingRate;
    }

    public void setShoppingRevolvingRate(BigDecimal shoppingRevolvingRate) {
        this.shoppingRevolvingRate = shoppingRevolvingRate;
    }

    public BigDecimal getCashingRate() {
        return cashingRate;
    }

    public void setCashingRate(BigDecimal cashingRate) {
        this.cashingRate = cashingRate;
    }

    public Integer getPaymentDay() {
        return paymentDay;
    }

    public void setPaymentDay(Integer paymentDay) {
        this.paymentDay = paymentDay;
    }

    public Integer getClosingDay() {
        return closingDay;
    }

    public void setClosingDay(Integer closingDay) {
        this.closingDay = closingDay;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
}
