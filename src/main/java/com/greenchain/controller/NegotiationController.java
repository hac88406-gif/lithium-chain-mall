package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.request.NegotiationSubmitRequest;
import com.greenchain.dto.response.NegotiationVO;
import com.greenchain.service.NegotiationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 商务洽谈控制器（客户端）
 */
@RestController
@RequestMapping("/api/client/negotiation")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationService negotiationService;

    /**
     * 提交洽谈
     * POST /api/client/negotiation/submit
     */
    @PostMapping("/submit")
    public Result<NegotiationVO> submit(@RequestBody NegotiationSubmitRequest request,
                                        HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        NegotiationVO vo = negotiationService.submit(userId, request);
        return Result.success("提交成功", vo);
    }

    /**
     * 根据订单ID查询洽谈记录
     * GET /api/client/negotiation/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public Result<NegotiationVO> getByOrderId(@PathVariable Long orderId) {
        NegotiationVO vo = negotiationService.getByOrderId(orderId);
        return Result.success(vo);
    }
}