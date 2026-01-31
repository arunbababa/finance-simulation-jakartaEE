package com.finance.simulation.resource;

import com.finance.simulation.model.*;
import com.finance.simulation.service.InterestCalculator;
import com.finance.simulation.service.LoanSimulationService;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * ローン返済シミュレーション REST API
 *
 * 【エンドポイント一覧】
 *
 * POST /api/simulation/calculate
 *   - 返済シミュレーションを実行
 *
 * GET /api/simulation/repayment-types
 *   - 利用可能な返済方式の一覧を取得
 *
 * GET /api/simulation/legal-rate?principal={amount}
 *   - 利息制限法に基づく上限金利を取得
 *
 * GET /api/simulation/balance-slide-types
 *   - 残高スライドテーブルの種類を取得
 */
@Path("/simulation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoanSimulationResource {

    @Inject
    private LoanSimulationService simulationService;

    @Inject
    private InterestCalculator interestCalculator;

    public LoanSimulationResource() {
        // CDI用のデフォルトコンストラクタ
    }

    /**
     * 返済シミュレーションを実行
     *
     * @param request シミュレーションパラメータ
     * @return シミュレーション結果
     */
    @POST
    @Path("/calculate")
    public Response calculate(@Valid LoanSimulationRequest request) {
        try {
            // サービスがInjectされていない場合の対応
            if (simulationService == null) {
                simulationService = new LoanSimulationService();
            }

            LoanSimulationResponse result = simulationService.simulate(request);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("シミュレーション中にエラーが発生しました: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 利用可能な返済方式の一覧を取得
     *
     * @return 返済方式のリスト
     */
    @GET
    @Path("/repayment-types")
    public Response getRepaymentTypes() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (RepaymentType type : RepaymentType.values()) {
            arrayBuilder.add(Json.createObjectBuilder()
                    .add("code", type.name())
                    .add("displayName", type.getDisplayName())
                    .add("description", type.getDescription())
                    .build());
        }

        return Response.ok(arrayBuilder.build().toString()).build();
    }

    /**
     * 利息制限法に基づく上限金利を取得
     *
     * @param principal 元本金額
     * @return 上限金利情報
     */
    @GET
    @Path("/legal-rate")
    public Response getLegalRate(@QueryParam("principal") BigDecimal principal) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("元本金額は0より大きい必要があります"))
                    .build();
        }

        if (interestCalculator == null) {
            interestCalculator = new InterestCalculator();
        }

        BigDecimal maxRate = interestCalculator.getLegalMaxRate(principal);

        JsonObject response = Json.createObjectBuilder()
                .add("principal", principal)
                .add("maxAnnualRate", maxRate)
                .add("maxAnnualRatePercent", maxRate.multiply(BigDecimal.valueOf(100)))
                .add("legalBasis", getLegalBasisDescription(principal))
                .add("reference", "利息制限法（昭和29年法律第100号）第1条")
                .add("referenceUrl", "https://elaws.e-gov.go.jp/document?lawid=329AC0000000100")
                .build();

        return Response.ok(response.toString()).build();
    }

    /**
     * 残高スライドテーブルの種類を取得
     *
     * @return 残高スライドテーブル情報
     */
    @GET
    @Path("/balance-slide-types")
    public Response getBalanceSlideTypes() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (BalanceSlideType type : BalanceSlideType.values()) {
            JsonObjectBuilder typeBuilder = Json.createObjectBuilder()
                    .add("code", type.name())
                    .add("displayName", type.getDisplayName());

            // テーブル詳細
            JsonArrayBuilder tableBuilder = Json.createArrayBuilder();
            BalanceSlideType.BalanceSlideTable table = type.getTable();
            BigDecimal[] thresholds = table.getThresholds();
            BigDecimal[] payments = table.getPayments();

            for (int i = 0; i <= thresholds.length; i++) {
                JsonObjectBuilder rowBuilder = Json.createObjectBuilder();
                if (i == 0) {
                    rowBuilder.add("balanceRange", "〜" + formatCurrency(thresholds[0]) + "円");
                } else if (i == thresholds.length) {
                    rowBuilder.add("balanceRange", formatCurrency(thresholds[i - 1]) + "円超");
                } else {
                    rowBuilder.add("balanceRange",
                            formatCurrency(thresholds[i - 1]) + "円超〜" + formatCurrency(thresholds[i]) + "円");
                }
                rowBuilder.add("monthlyPayment", payments[i]);
                tableBuilder.add(rowBuilder.build());
            }

            typeBuilder.add("table", tableBuilder.build());
            arrayBuilder.add(typeBuilder.build());
        }

        return Response.ok(arrayBuilder.build().toString()).build();
    }

    /**
     * 比較シミュレーション - 複数の返済方式を比較
     *
     * @param principal 借入金額
     * @param annualRate 年利
     * @return 各返済方式の比較結果
     */
    @GET
    @Path("/compare")
    public Response compareRepaymentTypes(
            @QueryParam("principal") BigDecimal principal,
            @QueryParam("annualRate") BigDecimal annualRate,
            @QueryParam("termMonths") Integer termMonths,
            @QueryParam("monthlyPayment") BigDecimal monthlyPayment) {

        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("借入金額は必須です"))
                    .build();
        }

        if (annualRate == null) {
            annualRate = LegalConstants.TYPICAL_REVOLVING_RATE;
        }

        if (simulationService == null) {
            simulationService = new LoanSimulationService();
        }

        JsonArrayBuilder resultsBuilder = Json.createArrayBuilder();

        // 元利均等返済（期間指定がある場合）
        if (termMonths != null && termMonths > 0) {
            try {
                LoanSimulationRequest req = LoanSimulationRequest.builder()
                        .principal(principal)
                        .annualRate(annualRate)
                        .repaymentType(RepaymentType.EQUAL_TOTAL_PAYMENT)
                        .termMonths(termMonths)
                        .build();
                LoanSimulationResponse res = simulationService.simulate(req);
                resultsBuilder.add(createComparisonResult(res));
            } catch (Exception e) {
                // 計算できない場合はスキップ
            }

            // 元金均等返済
            try {
                LoanSimulationRequest req = LoanSimulationRequest.builder()
                        .principal(principal)
                        .annualRate(annualRate)
                        .repaymentType(RepaymentType.EQUAL_PRINCIPAL_PAYMENT)
                        .termMonths(termMonths)
                        .build();
                LoanSimulationResponse res = simulationService.simulate(req);
                resultsBuilder.add(createComparisonResult(res));
            } catch (Exception e) {
                // 計算できない場合はスキップ
            }
        }

        // リボ払い（月々返済額指定がある場合）
        if (monthlyPayment != null && monthlyPayment.compareTo(BigDecimal.ZERO) > 0) {
            try {
                LoanSimulationRequest req = LoanSimulationRequest.builder()
                        .principal(principal)
                        .annualRate(annualRate)
                        .repaymentType(RepaymentType.REVOLVING_FIXED_AMOUNT)
                        .monthlyPayment(monthlyPayment)
                        .build();
                LoanSimulationResponse res = simulationService.simulate(req);
                resultsBuilder.add(createComparisonResult(res));
            } catch (Exception e) {
                // 計算できない場合はスキップ
            }
        }

        // 残高スライド
        try {
            LoanSimulationRequest req = LoanSimulationRequest.builder()
                    .principal(principal)
                    .annualRate(annualRate)
                    .repaymentType(RepaymentType.REVOLVING_BALANCE_SLIDE)
                    .balanceSlideType(BalanceSlideType.STANDARD)
                    .build();
            LoanSimulationResponse res = simulationService.simulate(req);
            resultsBuilder.add(createComparisonResult(res));
        } catch (Exception e) {
            // 計算できない場合はスキップ
        }

        // 分割払い（一般的な回数）
        for (int count : Arrays.asList(3, 6, 12, 24)) {
            try {
                LoanSimulationRequest req = LoanSimulationRequest.builder()
                        .principal(principal)
                        .annualRate(LegalConstants.TYPICAL_INSTALLMENT_RATE)
                        .repaymentType(RepaymentType.INSTALLMENT)
                        .installmentCount(count)
                        .build();
                LoanSimulationResponse res = simulationService.simulate(req);
                JsonObjectBuilder result = createComparisonResult(res);
                result.add("installmentCount", count);
                resultsBuilder.add(result.build());
            } catch (Exception e) {
                // 計算できない場合はスキップ
            }
        }

        JsonObject response = Json.createObjectBuilder()
                .add("principal", principal)
                .add("annualRate", annualRate)
                .add("results", resultsBuilder.build())
                .build();

        return Response.ok(response.toString()).build();
    }

    // ========================================
    // プライベートメソッド
    // ========================================

    private JsonObjectBuilder createComparisonResult(LoanSimulationResponse res) {
        return Json.createObjectBuilder()
                .add("repaymentType", res.getRepaymentType().name())
                .add("repaymentTypeName", res.getRepaymentType().getDisplayName())
                .add("totalPayment", res.getTotalPayment())
                .add("totalInterest", res.getTotalInterest())
                .add("totalMonths", res.getTotalMonths())
                .add("termDisplay", res.getTermDisplay())
                .add("monthlyPayment", res.getMonthlyPayment());
    }

    private String getLegalBasisDescription(BigDecimal principal) {
        if (principal.compareTo(LegalConstants.THRESHOLD_100K) < 0) {
            return "元本10万円未満のため、上限金利は年20%";
        } else if (principal.compareTo(LegalConstants.THRESHOLD_1M) < 0) {
            return "元本10万円以上100万円未満のため、上限金利は年18%";
        } else {
            return "元本100万円以上のため、上限金利は年15%";
        }
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }

    private JsonObject createErrorResponse(String message) {
        return Json.createObjectBuilder()
                .add("error", true)
                .add("message", message)
                .build();
    }
}
