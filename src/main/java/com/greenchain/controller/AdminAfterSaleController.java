package com.greenchain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.annotation.RequiresPermission;
import com.greenchain.common.BusinessException;
import com.greenchain.common.Result;
import com.greenchain.dto.request.AfterSaleReviewRequest;
import com.greenchain.dto.response.AfterSaleVO;
import com.greenchain.entity.AfterSale;
import com.greenchain.mapper.AfterSaleMapper;
import com.greenchain.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * 管理后台售后管理控制器
 * <p>
 * 使用 @RequiresPermission 注解 + PermissionInterceptor 实现权限校验，
 * 权限码：sys:aftersale:list（列表/详情）、sys:aftersale:review（审核）。
 * 审核同意且为"退款"类型时，联动 PaymentService 触发资金退款（Mock 通道）。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/afterSale")
public class AdminAfterSaleController {

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Autowired
    private PaymentService paymentService;

    /**
     * 售后分页列表（关联订单号、用户信息）
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param status   状态筛选：0待审核 1同意 2拒绝，不传查全部
     */
    @GetMapping("/list")
    @RequiresPermission(value = "sys:aftersale:list", description = "售后管理列表")
    public Result<IPage<AfterSaleVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Integer status) {
        Page<AfterSaleVO> page = afterSaleMapper.selectPageWithOrderAndUser(new Page<>(pageNum, pageSize), status);
        return Result.success(page);
    }

    /**
     * 售后详情（关联订单号、用户信息）
     */
    @GetMapping("/{id}")
    @RequiresPermission(value = "sys:aftersale:list", description = "售后管理列表")
    public Result<AfterSaleVO> detail(@PathVariable("id") Long id) {
        AfterSaleVO vo = afterSaleMapper.selectDetailById(id);
        if (vo == null) {
            throw new BusinessException(404, "售后单不存在");
        }
        return Result.success(vo);
    }

    /**
     * 审核售后：传入 afterSaleId、status（1同意 2拒绝）、rejectReason（拒绝时必填）
     * <p>
     * 仅更新售后单业务状态，不做库存回补、不做真实退款。
     */
    @PostMapping("/review")
    @RequiresPermission(value = "sys:aftersale:review", description = "售后审核")
    public Result<Void> review(@Valid @RequestBody AfterSaleReviewRequest request) {
        AfterSale afterSale = afterSaleMapper.selectById(request.getAfterSaleId());
        if (afterSale == null) {
            throw new BusinessException(404, "售后单不存在");
        }

        // 仅待审核状态可审核，防止重复/终态后再次修改
        if (afterSale.getStatus() == null || afterSale.getStatus() != AfterSale.STATUS_PENDING) {
            throw new BusinessException(400, "该售后单已审核，无法重复审核");
        }

        // 拒绝时拒绝理由必填
        if (request.getStatus() == AfterSale.STATUS_REJECTED
                && (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty())) {
            throw new BusinessException(400, "拒绝时必须填写拒绝理由");
        }

        afterSale.setStatus(request.getStatus());
        afterSale.setRejectReason(request.getStatus() == AfterSale.STATUS_REJECTED
                ? request.getRejectReason() : null);
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);

        // ===== 真实支付打通：同意"退款"类型售后时，联动支付通道发起资金退款 =====
        // 退款异常不阻断审核主流程（仅记录日志），保证售后单据状态先行落库。
        if (request.getStatus() == AfterSale.STATUS_APPROVED
                && afterSale.getAfterSaleType() != null
                && afterSale.getAfterSaleType() == AfterSale.TYPE_REFUND) {
            try {
                paymentService.refund(afterSale.getOrderId(), afterSale.getReason());
            } catch (Exception e) {
                log.error("售后同意退款时触发退款失败：afterSaleId={}, orderId={}, err={}",
                        afterSale.getId(), afterSale.getOrderId(), e.getMessage(), e);
            }
        }

        return Result.success("审核完成", null);
    }
}
