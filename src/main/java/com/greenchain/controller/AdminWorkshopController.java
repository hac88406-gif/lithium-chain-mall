package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.WorkshopVO;
import com.greenchain.entity.Workshop;
import com.greenchain.service.WorkshopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 车间管理控制器（管理员后台）
 */
@RestController
@RequestMapping("/api/admin/workshop")
@RequiredArgsConstructor
public class AdminWorkshopController {

    private final WorkshopService workshopService;

    /**
     * 查询车间列表
     * GET /api/admin/workshop/list
     */
    @GetMapping("/list")
    public Result<PageResult<WorkshopVO>> list(@RequestParam(defaultValue = "1") Long current,
                                               @RequestParam(defaultValue = "10") Long size) {
        PageResult<WorkshopVO> page = workshopService.listAll(current, size);
        return Result.success(page);
    }

    /**
     * 新增车间
     * POST /api/admin/workshop/add
     */
    @PostMapping("/add")
    public Result<WorkshopVO> add(@RequestBody Workshop workshop) {
        WorkshopVO vo = workshopService.add(workshop);
        return Result.success("新增成功", vo);
    }

    /**
     * 编辑车间
     * PUT /api/admin/workshop/update
     */
    @PutMapping("/update")
    public Result<WorkshopVO> update(@RequestBody Workshop workshop) {
        WorkshopVO vo = workshopService.update(workshop);
        return Result.success("编辑成功", vo);
    }

    /**
     * 删除车间
     * DELETE /api/admin/workshop/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workshopService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询车间详情
     * GET /api/admin/workshop/{id}
     */
    @GetMapping("/{id}")
    public Result<WorkshopVO> getById(@PathVariable Long id) {
        WorkshopVO vo = workshopService.getById(id);
        return Result.success(vo);
    }
}