package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.response.PaymentVO;
import com.greenchain.entity.Order;
import com.greenchain.entity.OrderItem;
import com.greenchain.entity.PaymentTransaction;
import com.greenchain.entity.Product;
import com.greenchain.mapper.OrderItemMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.mapper.PaymentTransactionMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.payment.PaySignature;
import com.greenchain.payment.PaymentChannel;
import com.greenchain.payment.PaymentProperties;
import com.greenchain.payment.dto.NotifyParseResult;
import com.greenchain.payment.dto.PrePayRequest;
import com.greenchain.payment.dto.PrePayResponse;
import com.greenchain.payment.dto.RefundRequest;
import com.greenchain.payment.dto.RefundResult;
import com.greenchain.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务实现：完整编排支付生命周期（预下单 → 回调验签 → 幂等 → 订单状态机 → 退款）。
 * <p>
 * 策略模式注入所有 {@link PaymentChannel}，按配置的 default-channel 选择当前通道（mock），
 * 未来接微信/支付宝只需新增实现并改配置，本类无需改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** Redis ZSet 热销榜 key（与 ClientOrderController 保持一致） */
    private static final String RANK_KEY_PRODUCT_SALES = "rank:product:sales";

    private final PaymentTransactionMapper paymentTransactionMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final com.greenchain.util.CacheUtil cacheUtil;
    private final List<PaymentChannel> channels;
    private final PaymentProperties properties;

    // ==================== 预下单 ====================

    @Override
    public PaymentVO createPayment(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态不可支付");
        }

        // 幂等：该订单已存在支付流水（成功/已退款）时直接返回既有结果，防止重复创建预下单
        PaymentTransaction exist = findPaymentByOrder(orderId);
        if (exist != null
                && (PaymentTransaction.STATUS_SUCCESS.equals(exist.getStatus())
                    || PaymentTransaction.STATUS_REFUNDED.equals(exist.getStatus()))) {
            return toVO(exist);
        }

        String paymentNo = generatePaymentNo();
        PaymentChannel channel = currentChannel();

        // 落一条预下单流水（status=created）
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId(order.getId());
        tx.setOrderNo(order.getOrderNo());
        tx.setUserId(order.getUserId());
        tx.setPaymentNo(paymentNo);
        tx.setChannel(channel.channel());
        tx.setAmount(order.getTotalAmount());
        tx.setType(PaymentTransaction.TYPE_PAYMENT);
        tx.setStatus(PaymentTransaction.STATUS_CREATED);
        tx.setCreateTime(LocalDateTime.now());
        tx.setUpdateTime(LocalDateTime.now());
        paymentTransactionMapper.insert(tx);

        // 调支付通道预下单
        PrePayRequest req = new PrePayRequest(paymentNo, order.getOrderNo(), order.getId(),
                order.getTotalAmount(), "绿链锂电订单-" + order.getOrderNo());
        PrePayResponse resp = channel.prePay(req);
        if (!resp.isSuccess()) {
            throw new BusinessException(500, "发起支付失败：" + resp.getErrMsg());
        }

        // 回写网关返回的交易号与签名（回调验签比对用）
        tx.setTransactionId(resp.getTransactionId());
        tx.setSign(resp.getSign());
        tx.setUpdateTime(LocalDateTime.now());
        paymentTransactionMapper.updateById(tx);

        log.info("发起预支付成功 paymentNo={} orderNo={} amount={}", paymentNo, order.getOrderNo(), order.getTotalAmount());
        return toVO(tx);
    }

    // ==================== 异步回调（验签 + 幂等 + 订单状态机） ====================

    @Override
    @Transactional
    public String handleNotify(Map<String, String> params, String sign) {
        PaymentChannel channel = currentChannel();

        // 1. 验签：签名不合法视为伪造回调，返回 FAIL
        NotifyParseResult parsed = channel.parseNotify(params, sign);
        if (!parsed.isValid()) {
            log.warn("支付回调验签失败：{}", parsed.getErrMsg());
            return "FAIL";
        }

        // 2. 幂等核心：把"预下单 created → success"只有一个请求能成功。
        //    重复/并发回调时受影响行数为 0，说明已处理过，直接回执成功避免重复扣减。
        int rows = paymentTransactionMapper.markPaySuccess(parsed.getPaymentNo(), parsed.getTransactionId());
        if (rows == 0) {
            log.info("支付回调重复到达，忽略幂等处理 paymentNo={}", parsed.getPaymentNo());
            return "SUCCESS";
        }

        // 3. 原子更新订单 pending → paid（按订单号，防把已取消订单改回已付款）
        int orderRows = orderMapper.markPaidByOrderNo(parsed.getOrderNo());
        if (orderRows == 0) {
            log.warn("支付回调成功但订单状态非可支付，可能存在状态异常 orderNo={}", parsed.getOrderNo());
            return "SUCCESS";
        }

        // 4. 累加商品销量 + 更新 Redis 热销榜（复用既有逻辑，失败不阻断主流程）
        incrementSalesByOrderNo(parsed.getOrderNo());

        log.info("支付回调成功 paymentNo={} orderNo={} amount={}",
                parsed.getPaymentNo(), parsed.getOrderNo(), parsed.getAmount());
        return "SUCCESS";
    }

    // ==================== 退款联动 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long orderId, String reason) {
        // 找到该订单原支付流水（必须是已支付成功）
        PaymentTransaction payTx = findPaidPaymentByOrder(orderId);
        if (payTx == null) {
            throw new BusinessException(400, "该订单没有可退款的支付记录");
        }

        PaymentChannel channel = currentChannel();
        String refundNo = generatePaymentNo();

        // 落一条退款流水（refunding）
        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setOrderId(payTx.getOrderId());
        refundTx.setOrderNo(payTx.getOrderNo());
        refundTx.setUserId(payTx.getUserId());
        refundTx.setPaymentNo(refundNo);
        refundTx.setChannel(channel.channel());
        refundTx.setAmount(payTx.getAmount());
        refundTx.setType(PaymentTransaction.TYPE_REFUND);
        refundTx.setStatus(PaymentTransaction.STATUS_REFUNDING);
        refundTx.setRefPaymentNo(payTx.getPaymentNo());
        refundTx.setCreateTime(LocalDateTime.now());
        refundTx.setUpdateTime(LocalDateTime.now());
        paymentTransactionMapper.insert(refundTx);

        // 调通道退款（Mock 直接成功）
        RefundRequest refundReq = new RefundRequest(payTx.getPaymentNo(), payTx.getTransactionId(),
                refundNo, payTx.getAmount(), reason == null ? "售后退款" : reason);
        RefundResult result = channel.refund(refundReq);
        if (!result.isSuccess()) {
            refundTx.setStatus(PaymentTransaction.STATUS_FAIL);
            refundTx.setUpdateTime(LocalDateTime.now());
            paymentTransactionMapper.updateById(refundTx);
            throw new BusinessException(500, "退款失败：" + result.getErrMsg());
        }

        // 幂等标记退款完成（refunding → refunded）
        paymentTransactionMapper.markRefunded(refundNo);
        log.info("退款成功 refundNo={} orderNo={} amount={}", refundNo, payTx.getOrderNo(), payTx.getAmount());
    }

    // ==================== 模拟支付（本地演示，无真实网关） ====================

    @Override
    public String simulatePay(String paymentNo) {
        PaymentTransaction tx = paymentTransactionMapper.selectOne(new QueryWrapper<PaymentTransaction>()
                .eq("payment_no", paymentNo).eq("type", PaymentTransaction.TYPE_PAYMENT));
        if (tx == null) {
            throw new BusinessException(404, "支付流水不存在");
        }
        // 构造与预下单签名规则完全一致的合法回调参数
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mch_id", "10000001");
        params.put("out_trade_no", tx.getOrderNo());
        params.put("payment_no", tx.getPaymentNo());
        params.put("transaction_id", tx.getTransactionId());
        params.put("total_fee", toFen(tx.getAmount()));
        params.put("time_stamp", LocalDateTime.now().format(TIME_FMT));
        params.put("nonce_str", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        String sign = PaySignature.sign(params, properties.getMockSecretKey());

        handleNotify(params, sign);

        Order order = orderMapper.selectById(tx.getOrderId());
        return order == null ? null : order.getStatus();
    }

    // ==================== 私有工具方法 ====================

    private PaymentChannel currentChannel() {
        return channels.stream()
                .filter(c -> c.channel().equals(properties.getDefaultChannel()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到支付通道: " + properties.getDefaultChannel()));
    }

    private PaymentTransaction findPaymentByOrder(Long orderId) {
        return paymentTransactionMapper.selectOne(new QueryWrapper<PaymentTransaction>()
                .eq("order_id", orderId).eq("type", PaymentTransaction.TYPE_PAYMENT)
                .orderByDesc("id").last("LIMIT 1"));
    }

    private PaymentTransaction findPaidPaymentByOrder(Long orderId) {
        PaymentTransaction tx = findPaymentByOrder(orderId);
        if (tx != null && PaymentTransaction.STATUS_SUCCESS.equals(tx.getStatus())) {
            return tx;
        }
        return null;
    }

    /**
     * 支付成功后累加商品销量并刷新热销榜（复刻原 ClientOrderController.payOrder 逻辑）。
     */
    private void incrementSalesByOrderNo(String orderNo) {
        Order order = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).last("LIMIT 1"));
        if (order == null) {
            return;
        }
        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setSales((product.getSales() != null ? product.getSales() : 0) + item.getQuantity());
                product.setUpdateTime(LocalDateTime.now());
                productMapper.updateById(product);
                // 热销榜销量实时累加（原子，失败不阻断主流程）
                try {
                    cacheUtil.zIncrementScore(RANK_KEY_PRODUCT_SALES, item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.warn("热销榜销量累加失败（不影响主流程）：productId={}, err={}", item.getProductId(), e.getMessage());
                }
            }
        }
    }

    private PaymentVO toVO(PaymentTransaction tx) {
        PaymentVO vo = new PaymentVO();
        vo.setOrderId(tx.getOrderId());
        vo.setOrderNo(tx.getOrderNo());
        vo.setPaymentNo(tx.getPaymentNo());
        vo.setAmount(tx.getAmount());
        vo.setChannel(tx.getChannel());
        vo.setStatus(tx.getStatus());
        // 支付链接本地演示用拼接值（真实通道应在预下单时持久化）
        vo.setPayUrl("mock-pay://pay?pay_no=" + tx.getPaymentNo());
        return vo;
    }

    private String generatePaymentNo() {
        return "pay" + LocalDateTime.now().format(TIME_FMT)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String toFen(BigDecimal yuan) {
        return (yuan == null ? BigDecimal.ZERO : yuan)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .toBigInteger().toString();
    }
}