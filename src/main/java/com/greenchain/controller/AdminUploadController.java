package com.greenchain.controller;

import com.greenchain.annotation.RequiresPermission;
import com.greenchain.common.Result;
import com.greenchain.util.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台文件上传控制器（MinIO 对象存储）
 * <p>
 * 使用场景：商品图片、车间图片等管理端图片上传。
 * 鉴权：/admin/** 由 AdminAuthInterceptor 校验管理员身份，
 * 上传动作额外要求 sys:upload:image 权限点（PermissionInterceptor 校验）。
 * <p>
 * 说明：项目原无本地上传实现（图片此前为静态资源路径 /images/*.jpg），
 * 本接口为新增的统一上传入口，数据库图片字段直接保存返回的 http url。
 */
@RestController
@RequestMapping("/api/admin/upload")
public class AdminUploadController {

    @Autowired
    private MinioUtil minioUtil;

    /**
     * 上传图片（商品图片、车间图片等共用）
     *
     * @param file 图片文件（仅允许 image/* 类型）
     * @return Result<String> 图片可访问 http url，前端赋值给表单字段后随商品/车间数据提交
     */
    @PostMapping("/image")
    @RequiresPermission(value = "sys:upload:image", description = "图片上传")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 仅允许图片类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持上传图片文件");
        }
        String url = minioUtil.uploadFile(file);
        return Result.success("上传成功", url);
    }

    /**
     * 删除文件（根据 uploadImage 返回的 url）
     * 供后续商品删除/图片替换时联动清理存储资源
     */
    @DeleteMapping("/file")
    @RequiresPermission(value = "sys:upload:image", description = "图片上传")
    public Result<Void> deleteFile(@RequestParam("fileUrl") String fileUrl) {
        minioUtil.deleteFile(fileUrl);
        return Result.success("删除成功", null);
    }
}
