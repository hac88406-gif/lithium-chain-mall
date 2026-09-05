package com.greenchain.config;

import com.greenchain.interceptor.AdminAuthInterceptor;
import com.greenchain.interceptor.ClientAuthInterceptor;
import com.greenchain.interceptor.OrderRateLimitInterceptor;
import com.greenchain.interceptor.PermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ClientAuthInterceptor clientAuthInterceptor;

    // 管理后台鉴权拦截器（B1 修复）：校验 JWT 并加载权限上下文
    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    // 权限拦截器（B1 修复）：让 @RequiresPermission 注解生效
    @Autowired
    private PermissionInterceptor permissionInterceptor;

    // 下单 IP 限流拦截器：同一 IP 1 分钟最多 5 次下单，防脚本刷单
    @Autowired
    private OrderRateLimitInterceptor orderRateLimitInterceptor;

    /**
     * 本地文件存储目录（默认项目根下 uploads/，可通过 upload.local-dir 配置覆盖）。
     * 不依赖 MinIO 即可工作，GET /uploads/xxx.png 直接访问磁盘文件。
     */
    @Value("${upload.local-dir:${user.dir}/uploads}")
    private String uploadLocalDir;

    /**
     * CORS 跨域配置
     * <p>
     * 默认允许所有来源（开发环境方便），生产环境应通过 cors.allowed-origins 配置限定具体域名。
     * P1-3 修复：allowedOriginPatterns 从配置读取，不硬编码 "*"。
     * 示例：cors.allowed-origins=http://localhost:5173,http://localhost:5174
     * 生产环境建议：cors.allowed-origins=https://yourdomain.com
     */
    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientAuthInterceptor)
                .addPathPatterns("/api/client/**")
                .excludePathPatterns("/api/client/auth/register", "/api/client/auth/login")
                .excludePathPatterns("/api/client/products/**")
                .excludePathPatterns("/api/client/categories/**")
                .excludePathPatterns("/api/client/carousel/**")
                .excludePathPatterns("/api/client/process/**")
                // Coze 智能客服：公开接口，未登录用户也能发起对话（sessionId 由前端生成）
                .excludePathPatterns("/api/client/coze/**")
                // 前台文件上传：售后凭证、合作申请附件等；纯存储操作，MinIO 后端隔离，
                // 无需强制登录拦截（管理员以 admin_token 登录时没有客户端 JWT，拦截会 401）
                .excludePathPatterns("/api/client/upload/**")
                // 商务合作 / 人工客服：允许未登录访客提交
                .excludePathPatterns("/api/client/cooperation/**")
                .excludePathPatterns("/api/client/service-request/**")
                // 支付网关异步回调：公开接口（模拟真实网关回调），安全由回调验签承担
                .excludePathPatterns("/api/client/payment/notify");

        // ===== 管理后台两段式拦截：鉴权 → 权限 =====

        // 1. 管理后台鉴权拦截器：校验 JWT、确认账号启用、加载权限上下文；
        //    登录接口排除（登录时还没有 token）
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");

        // 2. 权限拦截器：让 Controller 上的 @RequiresPermission 注解生效
        //    （依赖上一步写入的 PermissionContext，超管 admin 用户名豁免全部校验）
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/admin/**");

        // 下单 IP 限流拦截器：仅拦截【创建订单】这一个入口，防脚本刷单
        // 注意：不要拦截 /orders/** 子路径（pay/cancel/confirm/GET 查询），
        //       否则用户正常支付/取消/查订单也会被 429 限流（之前 2 笔订单已消耗 5 次配额导致无法支付）。
        registry.addInterceptor(orderRateLimitInterceptor)
                .addPathPatterns("/api/client/orders")         // 精确匹配：POST /api/client/orders 真实下单
                .addPathPatterns("/api/client/order/create");  // 兼容新版单数路径 OrderController.create
    }

    /**
     * 将本地 uploads/ 目录映射到 URL /uploads/**，浏览器可直接访问。
     * MinioUtil 默认落本地磁盘，不依赖 MinIO 运行即可。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadLocalDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // Windows 路径 file:/D:/.../uploads/，统一加末尾斜杠让 Spring 正确拼接子路径
        String location = dir.toURI().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}