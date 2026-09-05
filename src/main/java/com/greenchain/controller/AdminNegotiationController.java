package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.response.NegotiationVO;
import com.greenchain.dto.response.PageResult;
import com.greenchain.service.NegotiationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商务洽谈管理控制器（管理员后台）
 */
@RestController
@RequestMapping("/api/admin/negotiation")
@RequiredArgsConstructor
public class AdminNegotiationController {

    private final NegotiationService negotiationService;

    /**
     * 查询洽谈列表
     * GET /api/admin/negotiation/list
     */
    @GetMapping("/list")
    public Result<PageResult<NegotiationVO>> list(@RequestParam(defaultValue = "1") Long current,
                                                   @RequestParam(defaultValue = "10") Long size,
                                                   @RequestParam(required = false) String status) {
        PageResult<NegotiationVO> page = negotiationService.listAll(current, size, status);
        return Result.success(page);
    }

    /**
     * 管理员回复洽谈
     * POST /api/admin/negotiation/{id}/reply
     */
    @PostMapping("/{id}/reply")
    public Result<NegotiationVO> reply(@PathVariable Long id,
                                       @RequestBody java.util.Map<String, String> body) {
        String reply = body.get("reply");
        NegotiationVO vo = negotiationService.reply(id, reply);
        return Result.success("回复成功", vo);
    }

    /**
     * 更新洽谈状态
     * PUT /api/admin/negotiation/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<NegotiationVO> updateStatus(@PathVariable Long id,
                                              @RequestBody java.util.Map<String, String> body) {
        String status = body.get("status");
        NegotiationVO vo = negotiationService.updateStatus(id, status);
        return Result.success("状态更新成功", vo);
    }

    /**
     * 删除洽谈记录
     * DELETE /api/admin/negotiation/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        negotiationService.delete(id);
        return Result.success("删除成功");
    }
}