package com.greenchain.payment;

import com.greenchain.payment.dto.NotifyParseResult;
import com.greenchain.payment.dto.PrePayRequest;
import com.greenchain.payment.dto.PrePayResponse;
import com.greenchain.payment.dto.RefundRequest;
import com.greenchain.payment.dto.RefundResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock 支付网关通道实现（模拟微信/支付宝生命周期）
 * <p>
 * 本地无真实商户号/证书，用该实现完整仿真真实支付流程：
 * <ul>
 *   <li>{@code prePay}：生成交易号、拼接拉起支付参数并对参数签名（HMAC-SHA256）</li>
 *   <li>{@code parseNotify}：对异步回调参数验签，防伪造回调</li>
 *   <li>{@code refund}：模拟退款，生成退款交易号</li>
 * </ul>
 * 仅是支付"行为"的模拟，仍真实走一遍"预下单 → 支付 → 签名回调验证"这套生产级设计。
 */
@Component
public class MockPaymentChannel implements PaymentChannel {

    private static final DateTimeFormatter PAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private PaymentProperties properties;

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public PrePayResponse prePay(PrePayRequest req) {
        // 生成平台交易号：时间戳 + 随机串（模拟第三方返回的 trade_no）
        String transactionId = "txn_" + LocalDateTime.now().format(PAY_TIME)
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 构造拉起支付时需要回传给客户端、并在回调时参与签名的参数
        Map<String, String> signParams = new LinkedHashMap<>();
        signParams.put("mch_id", "10000001");                    // 模拟商户号
        signParams.put("out_trade_no", req.getOrderNo());        // 订单号
        signParams.put("payment_no", req.getPaymentNo());        // 平台支付流水号(幂等)
        signParams.put("transaction_id", transactionId);        // 平台交易号
        signParams.put("total_fee", toFen(req.getAmount()));     // 金额(分)
        signParams.put("time_stamp", LocalDateTime.now().format(PAY_TIME));
        signParams.put("nonce_str", UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        // 对拉起参数签名（模拟网关下单返回的 sign，客户端/回调共用同一规则验签）
        String sign = PaySignature.sign(signParams, properties.getMockSecretKey());

        // 模拟支付二维码/拉起链接（本地仅作展示，无真实跳转）
        String payUrl = "mock-pay://pay?pay_no=" + req.getPaymentNo()
                + "&out_trade_no=" + req.getOrderNo()
                + "&amount=" + req.getAmount() + "&transaction_id=" + transactionId;

        return PrePayResponse.success(req.getPaymentNo(), transactionId, req.getAmount(), payUrl, signParams, sign);
    }

    @Override
    public NotifyParseResult parseNotify(Map<String, String> params, String sign) {
        if (params == null || params.isEmpty()) {
            return NotifyParseResult.invalid("回调参数为空");
        }
        // 核心：验签。仅当签名与本地按同样规则计算的结果一致才视为可信回调。
        boolean ok = PaySignature.verify(params, sign, properties.getMockSecretKey());
        if (!ok) {
            return NotifyParseResult.invalid("回调签名校验失败");
        }
        String paymentNo = params.get("payment_no");
        String orderNo = params.get("out_trade_no");
        String transactionId = params.get("transaction_id");
        BigDecimal amount = fromFen(params.get("total_fee"));
        if (paymentNo == null || orderNo == null || transactionId == null || amount == null) {
            return NotifyParseResult.invalid("回调参数缺少流水号/订单号/交易号/金额");
        }
        return NotifyParseResult.ok(paymentNo, orderNo, amount, transactionId, params);
    }

    @Override
    public RefundResult refund(RefundRequest req) {
        // 模拟退款成功，生成退款交易号
        String refundTxnId = "rfd_" + LocalDateTime.now().format(PAY_TIME)
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return RefundResult.ok(req.getRefundNo(), refundTxnId, req.getAmount());
    }

    /**
     * 元转分（两位小数），用于签名/回调金额一致性比对。
     */
    private String toFen(BigDecimal yuan) {
        if (yuan == null) {
            return "0";
        }
        return yuan.setScale(2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .toBigInteger().toString();
    }

    /**
     * 分转元（字符串可能为 null 或非数字，返回 null 表示非法）。
     */
    private BigDecimal fromFen(String fen) {
        if (fen == null || fen.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}