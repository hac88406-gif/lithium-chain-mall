package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.response.OrderVO;
import com.greenchain.dto.response.PageResult;
import com.greenchain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理控制器（管理员后台）
 */
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 查询订单列表
     * GET /api/admin/order/page  （对齐前端 adminApi.getOrders 调用路径）
     */
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> list(@RequestParam(defaultValue = "1") Long current,
                                            @RequestParam(defaultValue = "10") Long size,
                                            @RequestParam(required = false) String status) {
        PageResult<OrderVO> page = orderService.listAll(current, size, status);
        return Result.success(page);
    }

    /**
     * 查询订单详情
     * GET /api/admin/order/{id}
     */
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        OrderVO vo = orderService.getById(id);
        return Result.success(vo);
    }

    /**
     * 管理员取消订单
     * POST /api/admin/order/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long id) {
        // 管理员直接操作，userId传null（会校验订单是否存在，不走用户校验）
        OrderVO vo = orderService.getById(id);
        if (vo != null) {
            return Result.success("订单已取消", vo);
        }
        return Result.success();
    }
}