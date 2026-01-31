# ローン返済シミュレーター

Jakarta EE 10 を使用した金融返済シミュレーションアプリケーションです。
利息制限法・貸金業法・割賦販売法に基づく返済シミュレーションを学習できます。

## 機能

### 返済シミュレーション
- **元利均等返済**: 毎月の返済額（元金＋利息）が一定
- **元金均等返済**: 毎月の元金返済額が一定
- **リボ払い（定額方式）**: 毎月一定額を返済
- **リボ払い（残高スライド方式）**: 残高に応じて返済額が変動
- **リボ払い（定率方式）**: 残高の一定割合を返済
- **分割払い**: 指定回数で均等分割
- **一括払い**: 翌月に全額返済
- **キャッシング**: 一括/リボ払い

### その他の機能
- 返済方式の比較
- 法的情報の参照（利息制限法、出資法、貸金業法、割賦販売法）
- カード情報のセキュリティガイドライン

## 技術スタック

- **Backend**: Jakarta EE 10 (JAX-RS, CDI, JPA, Bean Validation)
- **Frontend**: HTML5 + CSS3 + Vanilla JavaScript
- **Build**: Maven
- **Java**: 17+

## プロジェクト構造

```
src/
├── main/
│   ├── java/com/finance/simulation/
│   │   ├── model/          # エンティティ・モデル
│   │   │   ├── LegalConstants.java      # 法的定数
│   │   │   ├── RepaymentType.java       # 返済方式
│   │   │   ├── BalanceSlideType.java    # 残高スライド
│   │   │   ├── LoanSimulationRequest.java
│   │   │   ├── LoanSimulationResponse.java
│   │   │   ├── MonthlyPaymentDetail.java
│   │   │   ├── CardInfo.java            # カード情報
│   │   │   └── CardBrand.java
│   │   ├── service/        # ビジネスロジック
│   │   │   ├── InterestCalculator.java  # 利息計算
│   │   │   └── LoanSimulationService.java
│   │   ├── resource/       # REST API
│   │   │   ├── JaxRsApplication.java
│   │   │   ├── LoanSimulationResource.java
│   │   │   └── CardInfoResource.java
│   │   └── util/           # ユーティリティ
│   ├── webapp/
│   │   ├── index.html
│   │   ├── resources/
│   │   │   ├── css/style.css
│   │   │   └── js/app.js
│   │   └── WEB-INF/
│   │       ├── beans.xml
│   │       └── web.xml
│   └── resources/
└── test/
```

## ビルドと実行

### 前提条件
- JDK 17以上
- Maven 3.8以上
- Jakarta EE 10 対応アプリケーションサーバー（WildFly 27+, Payara 6+, GlassFish 7+ など）

### ビルド
```bash
mvn clean package
```

### デプロイ
生成された `target/loan-simulator.war` をアプリケーションサーバーにデプロイしてください。

### アクセス
```
http://localhost:8080/loan-simulator/
```

## API エンドポイント

### シミュレーション

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/simulation/calculate` | 返済シミュレーション実行 |
| GET | `/api/simulation/repayment-types` | 返済方式一覧 |
| GET | `/api/simulation/legal-rate?principal={amount}` | 法定上限金利取得 |
| GET | `/api/simulation/balance-slide-types` | 残高スライドテーブル |
| GET | `/api/simulation/compare` | 返済方式比較 |

### カード情報

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/card/brands` | カードブランド一覧 |
| POST | `/api/card/validate` | カード番号検証 |
| GET | `/api/card/demo` | デモ用カード情報 |
| GET | `/api/card/security-guidelines` | セキュリティガイドライン |

## 法的根拠

### 利息制限法（昭和29年法律第100号）
第1条に基づく上限金利:
| 元本 | 上限金利（年率） |
|------|-----------------|
| 10万円未満 | 20% |
| 10万円以上〜100万円未満 | 18% |
| 100万円以上 | 15% |

参照: https://elaws.e-gov.go.jp/document?lawid=329AC0000000100

### 出資法（出資の受入れ、預り金及び金利等の取締りに関する法律）
貸金業者の上限金利: 年20%

参照: https://elaws.e-gov.go.jp/document?lawid=329AC0000000195

### 貸金業法（昭和58年法律第32号）
総量規制: 個人向け貸付は年収の1/3まで

参照: https://elaws.e-gov.go.jp/document?lawid=358AC0000000032

### 割賦販売法（昭和36年法律第159号）
クレジットカードの分割払い・リボ払いに関する規制

参照: https://elaws.e-gov.go.jp/document?lawid=336AC0000000159

## 計算式

### 元利均等返済
```
PMT = P × r × (1 + r)^n ÷ ((1 + r)^n - 1)

P: 元本
r: 月利（年利 ÷ 12）
n: 返済回数
```

### 遅延損害金の上限（利息制限法第4条）
```
遅延損害金 ≦ 法定利息 × 1.46
```

## セキュリティに関する注意

### カード情報の取り扱い
このアプリケーションは**学習用**です。本番環境では以下を遵守してください：

1. **PCI DSS準拠**: カード情報を扱う場合は必須
2. **CVV/CVCの保存禁止**: 決済時の一時認証のみに使用
3. **トークン化の使用**: Stripe, PAY.JP等のサービス利用を推奨
4. **暗号化**: カード番号はAES-256で暗号化
5. **通信の暗号化**: TLS 1.3を使用

## ライセンス

このプロジェクトは学習・教育目的で作成されています。
実際の金融取引には使用しないでください。

## 免責事項

このシミュレーターは概算値を提供するものであり、実際の返済額とは異なる場合があります。
正確な返済計画については、金融機関にご相談ください。
