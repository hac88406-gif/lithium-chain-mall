package com.greenchain.payment;

import com.greenchain.payment.dto.NotifyParseResult;
import com.greenchain.payment.dto.PrePayRequest;
import com.greenchain.payment.dto.PrePayResponse;
import com.greenchain.payment.dto.RefundRequest;
import com.greenchain.payment.dto.RefundResult;

import java.util.Map;

/**
 * 支付通道策略接口
 * <p>
 * 策略模式核心设计：所有支付通道（Mock/微信/支付宝/其他）都实现该接口，
 * 业务层只依赖 {@code PaymentChannel}，通过配置选择具体实现，新增通道时业务层无需修改。
 * <p>
 * 面试亮点：
 * <ul>
 *   <li>开闭原则：新增通道无需修改原有代码</li>
 *   <li>依赖倒置：业务层依赖抽象不依赖具体实现</li>
 * </ul>
 */
public interface PaymentChannel {

    /**
     * 返回通道标识，用于 Spring {@code @Component} 自动识别，如 "mock"。
     * 与 {@code greenchain.payment.default-channel} 配置对齐，业务层注入所有实现，
     * 按 channel 名称找到当前要使用的通道。
     *
     * @return 通道唯一标识
     */
    String channel();

    /**
     * 预下单：向支付通道发起支付申请，获取拉起支付所需的参数。
     *
     * @param req 预下单请求（订单号/金额/标题）
     * @return 预下单响应（支付信息，含签名）
     */
    PrePayResponse prePay(PrePayRequest req);

    /**
     * 解析并验签支付网关异步回调通知。
     * <p>
     * 网关调用 {@code /api/client/payment/notify}，将原参数 map 和用户提供的 sign 传入，
     * 通道实现完成参数解析和签名校验，返回是否通过验签以及业务三要素（订单号/金额/交易号）。
     *
     * @param params 网关回调过来的所有请求参数
     * @param sign   回调参数携带的签名字符串
     * @return 验签结果
     */
    NotifyParseResult parseNotify(Map<String, String> params, String sign);

    /**
     * 发起退款申请。
     *
     * @param req 退款请求（原支付流水、退款金额、原因）
     * @return 退款结果
     */
    RefundResult refund(RefundRequest req);
}