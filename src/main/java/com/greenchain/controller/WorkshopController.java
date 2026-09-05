package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.WorkshopVO;
import com.greenchain.service.WorkshopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 车间控制器（客户端）
 * 提供3D车间数据查询
 */
@RestController
@RequestMapping("/api/client/workshop")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopService workshopService;

    /**
     * 查询车间列表
     * GET /api/client/workshop/list
     */
    @GetMapping("/list")
    public Result<PageResult<WorkshopVO>> list(@RequestParam(defaultValue = "1") Long current,
                                               @RequestParam(defaultValue = "10") Long size) {
        PageResult<WorkshopVO> page = workshopService.listAll(current, size);
        return Result.success(page);
    }

    /**
     * 查询车间详情（含3D数据和工序）
     * GET /api/client/workshop/{id}
     */
    @GetMapping("/{id}")
    public Result<WorkshopVO> getById(@PathVariable Long id) {
        WorkshopVO vo = workshopService.getById(id);
        return Result.success(vo);
    }

    /**
     * 获取车间3D数据
     * GET /api/client/workshop/{id}/3d
     */
    @GetMapping("/{id}/3d")
    public Result<WorkshopVO> get3DData(@PathVariable Long id) {
        WorkshopVO vo = workshopService.getById(id);
        return Result.success(vo);
    }
}