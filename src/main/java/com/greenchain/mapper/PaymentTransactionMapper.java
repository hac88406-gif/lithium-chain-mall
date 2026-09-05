package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付流水 Mapper
 */
@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {

    /**
     * 支付回调成功：把某笔"预下单"流水原子置为成功（幂等核心）。
     * <p>
     * 使用条件 UPDATE {@code status='created' → 'success'}：
     * 网关重复回调、或并发回调时，行级锁保证只有第一次回调把 created 置为 success，
     * 后续回调影响行数 = 0，业务层据此判定"已处理过"从而跳过重复扣减。
     *
     * @param paymentNo     平台支付流水号
     * @param transactionId 第三方交易号
     * @return 影响行数：1-本次成功置为已支付；0-已是终态（重复回调，忽略）
     */
    @Update("UPDATE payment_transaction SET status = 'success', transaction_id = #{transactionId}, " +
            "pay_time = NOW(), update_time = NOW() " +
            "WHERE payment_no = #{paymentNo} AND type = 'payment' AND status = 'created'")
    int markPaySuccess(@Param("paymentNo") String paymentNo,
                       @Param("transactionId") String transactionId);

    /**
     * 退款完成：把某笔退款流水置为已退款（幂等核心）。
     * <p>
     * {@code status='refunding' → 'refunded'}，重复退款/并发退款只有第一次生效。
     *
     * @param refundNo 退款流水号（paymentNo）
     * @return 影响行数：1-本次成功置为已退款；0-已处理过/状态不匹配
     */
    @Update("UPDATE payment_transaction SET status = 'refunded', refund_time = NOW(), update_time = NOW() " +
            "WHERE payment_no = #{refundNo} AND type = 'refund' AND status = 'refunding'")
    int markRefunded(@Param("refundNo") String refundNo);
}