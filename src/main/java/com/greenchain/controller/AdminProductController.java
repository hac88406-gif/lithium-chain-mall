package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.request.ProductQueryRequest;
import com.greenchain.dto.request.ProductSaveRequest;
import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.ProductVO;
import com.greenchain.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理控制器（管理员后台）
 */
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 商品分页查询（管理员可查所有状态）
     * GET /api/admin/product/page
     */
    @GetMapping("/page")
    public Result<PageResult<ProductVO>> pageQuery(ProductQueryRequest request) {
        // 管理员查看所有状态
        PageResult<ProductVO> page = productService.pageQuery(request);
        return Result.success(page);
    }

    /**
     * 新增商品
     * POST /api/admin/product/add
     */
    @PostMapping("/add")
    public Result<ProductVO> add(@RequestBody ProductSaveRequest request) {
        ProductVO vo = productService.add(request);
        return Result.success("新增成功", vo);
    }

    /**
     * 编辑商品
     * PUT /api/admin/product/update
     */
    @PutMapping("/update")
    public Result<ProductVO> update(@RequestBody ProductSaveRequest request) {
        ProductVO vo = productService.update(request);
        return Result.success("编辑成功", vo);
    }

    /**
     * 上架商品
     * PUT /api/admin/product/{id}/on-shelf
     */
    @PutMapping("/{id}/on-shelf")
    public Result<Void> onShelf(@PathVariable Long id) {
        productService.updateStatus(id, "ON_SHELF");
        return Result.success("上架成功");
    }

    /**
     * 下架商品
     * PUT /api/admin/product/{id}/off-shelf
     */
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.updateStatus(id, "OFF_SHELF");
        return Result.success("下架成功");
    }

    /**
     * 删除商品
     * DELETE /api/admin/product/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询商品详情
     * GET /api/admin/product/{id}
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO vo = productService.getById(id);
        return Result.success(vo);
    }
}