package com.greenchain.util;

import com.greenchain.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储工具类（本地磁盘实现，MinIO 可选）
 * <p>
 * 默认落本地磁盘（项目根目录下 ./uploads/{yyyy-MM-dd}/xxx.png），
 * 通过 WebMvcConfig 的 addResourceHandlers 映射到 URL /uploads/**，
 * 浏览器可直接访问，无需启动 MinIO。
 * <p>
 * 如需切换 MinIO：在 application.yml 配置 upload.backend=minio，
 * 并保证 MinIO 服务可连。当前实现暂不引入，MinIO 相关字段（endpoint/accessKey/secretKey/bucket）
 * 保留在 yml 中以便未来启用。
 */
@Slf4j
@Component
public class MinioUtil {

    @Value("${upload.local-dir:${user.dir}/uploads}")
    private String uploadLocalDir;

    /** 对外暴露的 URL 前缀，默认空串 → 路径形如 /uploads/2026-09-02/uuid.png */
    @Value("${upload.url-prefix:}")
    private String urlPrefix;

    private Path basePath;

    @PostConstruct
    public void init() {
        this.basePath = Paths.get(uploadLocalDir);
        try {
            Files.createDirectories(basePath);
            log.info("文件存储初始化：本地目录 = {}", basePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("创建 uploads 目录失败：{}", e.getMessage(), e);
            throw new BusinessException(500, "文件存储初始化失败");
        }
    }

    /**
     * 上传文件到本地磁盘，返回可直接访问的 URL。
     *
     * @param file 上传的文件
     * @return 对外 URL，形如 /uploads/2026-09-02/{uuid}.png；
     * 配置了 upload.url-prefix 时形如 https://cdn.example.com/uploads/...
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.lastIndexOf('.') >= 0) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path targetDir = basePath.resolve(dateDir);
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("本地文件上传失败：{}", e.getMessage(), e);
            throw new BusinessException(500, "文件上传失败，请稍后重试");
        }

        String relativeUrl = "/uploads/" + dateDir + "/" + fileName;
        String url = (urlPrefix != null && !urlPrefix.isEmpty())
                ? urlPrefix + relativeUrl
                : relativeUrl;
        log.info("本地文件上传成功：{}", targetFile);
        return url;
    }

    /**
     * 删除本地磁盘上的文件。url 必须以 /uploads/ 开头，防止拼接外部路径误删。
     *
     * @param fileUrl uploadFile 返回的 URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new BusinessException(400, "文件url不能为空");
        }
        // 统一去掉 url-prefix
        String prefix = "/uploads/";
        int idx = fileUrl.indexOf(prefix);
        if (idx < 0) {
            throw new BusinessException(400, "非法的文件地址，仅支持删除本地上传资源");
        }
        String relativePath = fileUrl.substring(idx + prefix.length());
        Path target = basePath.resolve(relativePath).normalize();
        // 安全校验：防止 ../ 路径逃逸出 uploads 目录
        if (!target.startsWith(basePath.normalize())) {
            throw new BusinessException(400, "非法的文件地址");
        }
        try {
            Files.deleteIfExists(target);
            log.info("本地文件删除成功：{}", target);
        } catch (IOException e) {
            log.error("本地文件删除失败：{}", e.getMessage(), e);
            throw new BusinessException(500, "文件删除失败");
        }
    }
}
