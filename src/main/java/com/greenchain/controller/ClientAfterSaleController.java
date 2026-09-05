package com.greenchain.controller;

import com.greenchain.common.BusinessException;
import com.greenchain.common.Result;
import com.greenchain.dto.request.AfterSaleApplyRequest;
import com.greenchain.dto.response.AfterSaleVO;
import com.greenchain.entity.AfterSale;
import com.greenchain.entity.Order;
import com.greenchain.mapper.AfterSaleMapper;
import com.greenchain.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 买家端售后接口
 * <p>
 * 归属 /api/client/** 拦截范围，未加入拦截器排除路径，必须携带有效 JWT token，
 * userId 一律从 JWT 解析（@RequestAttribute）获取。
 * 本模块仅做业务状态流转，不做真实资金退款、不做库存回补。
 */
@RestController
@RequestMapping("/api/client/afterSale")
@Validated
public class ClientAfterSaleController {

    /** 允许申请售后的订单状态：已付款 / 已发货 / 已完成（确认收货）——只要付款了就可以申请 */
    private static final java.util.Set<String> APPLY_ALLOWED_STATUSES = java.util.Set.of("paid", "shipped", "completed");

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 发起售后申请
     * <p>
     * 约束：
     * ① 订单必须属于当前登录用户（防越权）；
     * ② 订单状态必须为 completed（已确认收货）；
     * ③ 一个订单仅允许提交一次售后申请。
     *
     * @param userId    当前登录用户ID（JWT 解析）
     * @param applyReq  申请参数（@Valid 触发参数校验）
     * @return Result 统一返回
     */
    @PostMapping("/apply")
    public Result<AfterSaleVO> apply(@RequestAttribute("userId") Long userId,
                                     @Valid @RequestBody AfterSaleApplyRequest applyReq) {
        // ① 校验订单存在且属于当前用户
        Order order = orderMapper.selectById(applyReq.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }

        // ② 订单状态必须为已付款 / 已发货 / 已完成中的一种（只要已付款就允许申请售后）
        if (!APPLY_ALLOWED_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(400, "该订单状态不允许申请售后，请先完成付款");
        }

        // ③ 订单没有"进行中/已同意"的售后记录（被拒绝的可以重新申请）
        if (afterSaleMapper.findActiveByOrderId(applyReq.getOrderId()) != null) {
            throw new BusinessException(400, "该订单已有进行中的售后申请，请等待处理完成");
        }

        AfterSale afterSale = new AfterSale();
        afterSale.setOrderId(applyReq.getOrderId());
        afterSale.setUserId(userId);
        afterSale.setAfterSaleType(applyReq.getAfterSaleType());
        afterSale.setReason(applyReq.getReason());
        afterSale.setEvidence(applyReq.getEvidence());
        afterSale.setStatus(AfterSale.STATUS_PENDING);
        afterSale.setCreateTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.insert(afterSale);

        return Result.success("售后申请提交成功", toVO(afterSale, order));
    }

    /**
     * 查询我的售后列表（按申请时间倒序）
     */
    @GetMapping("/myList")
    public Result<List<AfterSaleVO>> myList(@RequestAttribute("userId") Long userId) {
        List<AfterSale> list = afterSaleMapper.findByUserId(userId);
        List<AfterSaleVO> voList = new java.util.ArrayList<>();
        for (AfterSale item : list) {
            Order order = orderMapper.selectById(item.getOrderId());
            voList.add(toVO(item, order));
        }
        return Result.success("查询成功", voList);
    }

    /**
     * 查询售后详情
     * 校验归属：只能查看自己的售后单
     */
    @GetMapping("/{id}")
    public Result<AfterSaleVO> detail(@RequestAttribute("userId") Long userId,
                                      @PathVariable("id") Long id) {
        AfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw new BusinessException(404, "售后单不存在");
        }
        if (!afterSale.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该售后单");
        }
        Order order = orderMapper.selectById(afterSale.getOrderId());
        return Result.success("查询成功", toVO(afterSale, order));
    }

    /**
     * 实体转 VO（补充订单号，供买家端展示）
     */
    private AfterSaleVO toVO(AfterSale afterSale, Order order) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setId(afterSale.getId());
        vo.setOrderId(afterSale.getOrderId());
        vo.setUserId(afterSale.getUserId());
        vo.setAfterSaleType(afterSale.getAfterSaleType());
        vo.setReason(afterSale.getReason());
        vo.setEvidence(afterSale.getEvidence());
        vo.setStatus(afterSale.getStatus());
        vo.setRejectReason(afterSale.getRejectReason());
        vo.setCreateTime(afterSale.getCreateTime());
        vo.setUpdateTime(afterSale.getUpdateTime());
        vo.setOrderNo(order != null ? order.getOrderNo() : null);
        return vo;
    }
}
