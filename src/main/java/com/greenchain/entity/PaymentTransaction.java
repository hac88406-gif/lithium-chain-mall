package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体（表 payment_transaction）
 * <p>
 * 记录一次支付申请/退款的全过程状态，是支付幂等的落点：
 * <ul>
 *   <li>事务类型 {@code type}：payment=支付、refund=退款</li>
 *   <li>流水状态 {@code status}：created=已预下单(待支付)、success=支付/退款成功、
 *       fail=失败、refunding=退款中、refunded=已退款、closed=已关闭(未支付取消)</li>
 * </ul>
 * 幂等保证：
 * <ul>
 *   <li>{@code payment_no} 唯一索引：同一订单重复发起支付只落一条预下单流水</li>
 *   <li>{@code transaction_id} 唯一索引：同一笔第三方交易只处理一次</li>
 *   <li>状态条件 UPDATE：{@code status='created' → 'success'} 只允许一次，回调重复到达时影响行数为 0</li>
 * </ul>
 */
@Data
@TableName("payment_transaction")
public class PaymentTransaction {

    /** 事务类型：支付 */
    public static final String TYPE_PAYMENT = "payment";
    /** 事务类型：退款 */
    public static final String TYPE_REFUND = "refund";

    /** 状态：已预下单，等待支付 */
    public static final String STATUS_CREATED = "created";
    /** 状态：支付/退款成功 */
    public static final String STATUS_SUCCESS = "success";
    /** 状态：支付/退款失败 */
    public static final String STATUS_FAIL = "fail";
    /** 状态：退款处理中 */
    public static final String STATUS_REFUNDING = "refunding";
    /** 状态：已退款 */
    public static final String STATUS_REFUNDED = "refunded";
    /** 状态：已关闭（未支付被取消） */
    public static final String STATUS_CLOSED = "closed";

    /** 流水ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 订单号（order.order_no） */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 平台支付流水号（幂等主键，唯一） */
    private String paymentNo;

    /** 支付通道标识（mock） */
    private String channel;

    /** 第三方交易号（唯一，幂等键） */
    private String transactionId;

    /** 金额（元） */
    private BigDecimal amount;

    /** 事务类型：payment 支付 / refund 退款 */
    private String type;

    /** 关联原支付流水号（退款单指向原支付单） */
    private String refPaymentNo;

    /** 流水状态：created/success/fail/refunding/refunded/closed */
    private String status;

    /** 预下单时返回的签名参数（json）与签名，供演示/调试 */
    private String signParams;

    /** 回调/发起时的签名 */
    private String sign;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 退款完成时间 */
    private LocalDateTime refundTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}