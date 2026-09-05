package com.greenchain.service;

import com.greenchain.dto.response.PaymentVO;

import java.util.Map;

/**
 * 支付服务：编排"预下单 → 支付 → 异步回调(验签+幂等) → 订单状态机 → 退款"完整生命周期。
 */
public interface PaymentService {

    /**
     * 发起预支付（创建支付流水）。
     * 幂等：同订单已支付/已退款时直接返回既有流水，不重复创建。
     *
     * @param userId  当前用户ID
     * @param orderId 订单ID
     * @return 支付信息（paymentNo、金额、支付链接）
     */
    PaymentVO createPayment(Long userId, Long orderId);

    /**
     * 处理支付网关异步回调（验签 + 幂等 + 原子更新订单状态 + 销量/热销榜）。
     *
     * @param params 回调参数（含 sign）
     * @param sign   回调签名
     * @return "SUCCESS"（可安全回执网关）/ "FAIL"（验签失败，网关重发或拒绝）
     */
    String handleNotify(Map<String, String> params, String sign);

    /**
     * 处理退款（售后"同意退款"时调用）。
     *
     * @param orderId 订单ID
     * @param reason  退款原因
     */
    void refund(Long orderId, String reason);

    /**
     * 模拟用户在支付网关"支付成功"（本地演示，无需真实网关）。
     * 构造带合法签名的回调参数，走一遍完整的 notify 流程。
     *
     * @param paymentNo 平台支付流水号
     * @return 支付成功后订单最终状态
     */
    String simulatePay(String paymentNo);
}