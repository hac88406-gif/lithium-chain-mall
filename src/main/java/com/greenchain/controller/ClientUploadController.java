package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.util.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 前台文件上传控制器（MinIO 对象存储）
 * <p>
 * 使用场景：合作申请（Negotiation）附件上传等前台场景。
 * 鉴权：归属 /api/client/** 拦截范围，未加入拦截器排除路径，
 * 必须携带有效 JWT token（ClientAuthInterceptor），未登录游客不能上传。
 * <p>
 * 说明：Negotiation 实体当前无附件字段（遵循"不新增数据库字段"约束），
 * 本接口仅负责文件上传并返回 http url；url 由前端持有，
 * 待后续 Negotiation 扩展附件字段后随申请数据落库。
 */
@RestController
@RequestMapping("/api/client/upload")
public class ClientUploadController {

    /** 允许上传的附件类型（图片 + 常见文档） */
    private static final Set<String> ALLOWED_TYPES = new java.util.HashSet<>(java.util.Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ));

    @Autowired
    private MinioUtil minioUtil;

    /**
     * 上传合作申请附件
     *
     * @param file 附件文件（图片或 pdf/word/excel，上限 10MB 由 yml multipart 配置控制）
     * @return Result<String> 附件可访问 http url
     */
    @PostMapping("/attachment")
    public Result<String> uploadAttachment(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return Result.error(400, "不支持的文件类型");
        }
        String url = minioUtil.uploadFile(file);
        return Result.success("上传成功", url);
    }
}
