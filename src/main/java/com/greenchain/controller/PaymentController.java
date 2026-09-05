package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.response.PaymentVO;
import com.greenchain.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 * <p>
 * 三入口：
 * <ul>
 *   <li>{@code POST /api/client/payment/create}：客户端发起预下单（需登录）</li>
 *   <li>{@code POST /api/client/payment/notify}：支付网关异步回调（公开、不鉴权，安全由验签承担）</li>
 *   <li>{@code POST /api/client/payment/{paymentNo}/simulate-pay}：本地演示，模拟用户"支付成功"（需登录）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/client/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 发起预下单。请求体：{"orderId": 123}
     */
    @PostMapping("/create")
    public Result<PaymentVO> create(@RequestBody Map<String, Object> body,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Object orderIdObj = body.get("orderId");
        Long orderId = ((Number) orderIdObj).longValue();
        PaymentVO vo = paymentService.createPayment(userId, orderId);
        return Result.success("发起支付成功", vo);
    }

    /**
     * 支付网关异步回调入口（公开接口，已在鉴权拦截器中放行）。
     * 网关以表单 POST 回调，参数全量在 form body 中，签名校验失败返回 FAIL、合法返回 SUCCESS。
     */
    @PostMapping("/notify")
    public Map<String, Object> notify(@RequestParam Map<String, String> params) {
        String sign = params.get("sign");
        String result = paymentService.handleNotify(params, sign);
        // 网关侧约定：返回 SUCCESS 表示处理成功不再重发；FAIL 表示处理失败网关会重试
        Map<String, Object> resp = new HashMap<>();
        resp.put("result", result);
        return resp;
    }

    /**
     * 本地演示：模拟用户在支付网关点击"支付成功"。
     * 构造带合法签名的回调参数并走一遍完整 notify 流程，返回订单最终状态。
     */
    @PostMapping("/{paymentNo}/simulate-pay")
    public Result<Map<String, String>> simulatePay(@PathVariable String paymentNo) {
        String status = paymentService.simulatePay(paymentNo);
        Map<String, String> data = new HashMap<>();
        data.put("orderStatus", status);
        data.put("message", "已模拟支付成功");
        return Result.success("支付完成", data);
    }
}