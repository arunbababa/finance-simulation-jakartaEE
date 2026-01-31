package com.finance.simulation.resource;

import com.finance.simulation.model.CardBrand;
import com.finance.simulation.model.CardInfo;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * カード情報管理 REST API
 *
 * 【セキュリティ上の重要な注意事項】
 *
 * このAPIは学習・デモ用です。本番環境では以下を必ず実装してください：
 *
 * 1. 認証・認可
 *    - OAuth 2.0 / OpenID Connect による認証
 *    - ロールベースのアクセス制御
 *
 * 2. 通信の暗号化
 *    - TLS 1.3 による通信
 *    - 証明書のピン留め
 *
 * 3. PCI DSS 準拠
 *    - カード番号の非保持化
 *    - トークン化の利用
 *    - 監査ログの記録
 *
 * 4. 入力検証
 *    - すべての入力パラメータの検証
 *    - SQLインジェクション対策
 *    - XSS対策
 *
 * 【エンドポイント一覧】
 *
 * GET /api/card/brands
 *   - カードブランド一覧を取得
 *
 * POST /api/card/validate
 *   - カード番号の形式を検証（Luhnチェック）
 *
 * GET /api/card/demo
 *   - デモ用のカード情報を取得
 */
@Path("/card")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CardInfoResource {

    /**
     * カードブランド一覧を取得
     *
     * @return カードブランドのリスト
     */
    @GET
    @Path("/brands")
    public Response getCardBrands() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (CardBrand brand : CardBrand.values()) {
            arrayBuilder.add(Json.createObjectBuilder()
                    .add("code", brand.name())
                    .add("displayName", brand.getDisplayName())
                    .add("prefixes", brand.getPrefixes())
                    .build());
        }

        return Response.ok(arrayBuilder.build().toString()).build();
    }

    /**
     * カード番号の形式を検証
     *
     * Luhnアルゴリズムによるチェックサム検証を行います。
     *
     * @param cardNumber カード番号
     * @return 検証結果
     */
    @POST
    @Path("/validate")
    public Response validateCardNumber(JsonObject request) {
        String cardNumber = request.getString("cardNumber", "");

        // 数字以外を除去
        String digits = cardNumber.replaceAll("[^0-9]", "");

        // 桁数チェック
        if (digits.length() < 13 || digits.length() > 19) {
            return Response.ok(Json.createObjectBuilder()
                    .add("valid", false)
                    .add("message", "カード番号は13〜19桁である必要があります")
                    .build().toString()).build();
        }

        // Luhnチェック
        boolean isValid = validateLuhn(digits);

        // ブランド判定
        String brand = detectBrand(digits);

        JsonObject response = Json.createObjectBuilder()
                .add("valid", isValid)
                .add("brand", brand)
                .add("maskedNumber", maskCardNumber(digits))
                .add("message", isValid ? "有効なカード番号形式です" : "カード番号のチェックサムが不正です")
                .build();

        return Response.ok(response.toString()).build();
    }

    /**
     * デモ用のカード情報を取得
     *
     * 学習用にサンプルデータを返します。
     * 実際のカード番号ではありません。
     *
     * @return デモ用カード情報
     */
    @GET
    @Path("/demo")
    public Response getDemoCardInfo() {
        // テスト用カード番号（実際には使用不可）
        CardInfo demoCard = new CardInfo();
        demoCard.setId(1L);
        demoCard.setCardHolderName("TARO YAMADA");
        demoCard.setCardNumber("4111111111111111"); // Visa テスト番号
        demoCard.setExpiryDate(YearMonth.of(2028, 12));
        demoCard.setShoppingLimit(new BigDecimal("500000"));
        demoCard.setCashingLimit(new BigDecimal("100000"));
        demoCard.setRevolvingLimit(new BigDecimal("500000"));
        demoCard.setShoppingBalance(new BigDecimal("150000"));
        demoCard.setCashingBalance(new BigDecimal("30000"));
        demoCard.setRevolvingBalance(new BigDecimal("80000"));
        demoCard.setShoppingRevolvingRate(new BigDecimal("0.15"));
        demoCard.setCashingRate(new BigDecimal("0.18"));
        demoCard.setPaymentDay(27);
        demoCard.setClosingDay(15);

        JsonObject response = Json.createObjectBuilder()
                .add("disclaimer", "これはデモ用のサンプルデータです。実際のカード情報ではありません。")
                .add("cardInfo", Json.createObjectBuilder()
                        .add("id", demoCard.getId())
                        .add("cardHolderName", demoCard.getCardHolderName())
                        .add("maskedCardNumber", demoCard.getMaskedCardNumber())
                        .add("cardBrand", demoCard.getCardBrand().getDisplayName())
                        .add("expiryDate", demoCard.getExpiryDate().toString())
                        .add("isValid", demoCard.isValid())
                        .add("shoppingLimit", demoCard.getShoppingLimit())
                        .add("cashingLimit", demoCard.getCashingLimit())
                        .add("revolvingLimit", demoCard.getRevolvingLimit())
                        .add("shoppingBalance", demoCard.getShoppingBalance())
                        .add("cashingBalance", demoCard.getCashingBalance())
                        .add("revolvingBalance", demoCard.getRevolvingBalance())
                        .add("availableShoppingLimit", demoCard.getAvailableShoppingLimit())
                        .add("availableCashingLimit", demoCard.getAvailableCashingLimit())
                        .add("shoppingRevolvingRate", demoCard.getShoppingRevolvingRate())
                        .add("cashingRate", demoCard.getCashingRate())
                        .add("paymentDay", demoCard.getPaymentDay())
                        .add("closingDay", demoCard.getClosingDay())
                        .build())
                .add("securityNotes", Json.createArrayBuilder()
                        .add("カード番号は暗号化して保存します")
                        .add("CVV/CVCはPCI DSSにより保存禁止です")
                        .add("本番環境ではトークン化を使用してください")
                        .add("通信は必ずTLS 1.3で暗号化してください")
                        .build())
                .build();

        return Response.ok(response.toString()).build();
    }

    /**
     * カード情報保存のベストプラクティス
     *
     * @return セキュリティガイドライン
     */
    @GET
    @Path("/security-guidelines")
    public Response getSecurityGuidelines() {
        JsonObject response = Json.createObjectBuilder()
                .add("title", "クレジットカード情報のセキュリティガイドライン")
                .add("guidelines", Json.createArrayBuilder()

                        // PCI DSS
                        .add(Json.createObjectBuilder()
                                .add("category", "PCI DSS準拠")
                                .add("description", "カード情報を扱う場合はPCI DSSへの準拠が必要")
                                .add("requirements", Json.createArrayBuilder()
                                        .add("セキュリティネットワークの構築と維持")
                                        .add("カード会員データの保護")
                                        .add("脆弱性管理プログラムの維持")
                                        .add("アクセス制御措置の実施")
                                        .add("ネットワークの監視とテスト")
                                        .add("情報セキュリティポリシーの維持")
                                        .build())
                                .add("reference", "https://www.pcisecuritystandards.org/")
                                .build())

                        // カード番号の保存
                        .add(Json.createObjectBuilder()
                                .add("category", "カード番号の保存")
                                .add("recommendations", Json.createArrayBuilder()
                                        .add("トークン化を使用（Stripe, PAY.JP等のサービス利用）")
                                        .add("カード番号は保存しない（非保持化）")
                                        .add("やむを得ず保存する場合はAES-256で暗号化")
                                        .add("暗号鍵はHSM（Hardware Security Module）で管理")
                                        .build())
                                .build())

                        // CVV/CVC
                        .add(Json.createObjectBuilder()
                                .add("category", "CVV/CVC（セキュリティコード）")
                                .add("rule", "絶対に保存してはいけません")
                                .add("reason", "PCI DSSで保存が明確に禁止されています")
                                .add("usage", "決済時の一時的な認証にのみ使用")
                                .build())

                        // 表示
                        .add(Json.createObjectBuilder()
                                .add("category", "カード番号の表示")
                                .add("rule", "常にマスキングして表示")
                                .add("example", "**** **** **** 1234")
                                .add("showDigits", "最後の4桁のみ表示可")
                                .build())

                        // 通信
                        .add(Json.createObjectBuilder()
                                .add("category", "通信のセキュリティ")
                                .add("requirements", Json.createArrayBuilder()
                                        .add("TLS 1.2以上（推奨はTLS 1.3）")
                                        .add("適切な暗号スイートの使用")
                                        .add("証明書のピン留め")
                                        .add("HTTPSの強制（HSTS）")
                                        .build())
                                .build())

                        .build())
                .build();

        return Response.ok(response.toString()).build();
    }

    // ========================================
    // プライベートメソッド
    // ========================================

    /**
     * Luhnアルゴリズムによる検証
     */
    private boolean validateLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(digits.charAt(i));
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
     * カードブランドを判定
     */
    private String detectBrand(String digits) {
        if (digits.startsWith("4")) {
            return "VISA";
        } else if (digits.startsWith("5") || digits.startsWith("2")) {
            return "MASTERCARD";
        } else if (digits.startsWith("35")) {
            return "JCB";
        } else if (digits.startsWith("34") || digits.startsWith("37")) {
            return "AMEX";
        } else if (digits.startsWith("36") || digits.startsWith("38") || digits.startsWith("39")) {
            return "DINERS";
        } else if (digits.startsWith("6011") || digits.startsWith("65")) {
            return "DISCOVER";
        } else if (digits.startsWith("62")) {
            return "UNIONPAY";
        }
        return "UNKNOWN";
    }

    /**
     * カード番号をマスキング
     */
    private String maskCardNumber(String digits) {
        if (digits.length() < 4) {
            return "****";
        }
        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
