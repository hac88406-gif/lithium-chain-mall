package com.greenchain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Knife4j 接口文档配置（访问地址：http://localhost:8080/doc.html）
 * <p>
 * 分组：client 前台接口、admin 后台接口两组，仅扫描 /api/** 业务路径，
 * 非业务路径（内部工具接口）不暴露；如需隐藏某个接口，可在其方法上加 @ApiIgnore。
 * <p>
 * 已开启全局 Authorization 请求头（示例值：Bearer xxx），
 * 在文档右上角"Authorize"填入 token 后，该分组下所有接口调试自动携带。
 * 原有全局异常与 Result 统一返回结构不受影响，文档按实际响应展示。
 * <p>
 * 说明：springfox-boot-starter 3.0.0 通过 spring.factories 自动装配启用，
 * 无需手动添加 @EnableSwagger2WebMvc 注解（knife4j 4.4.0 不传递该模块）。
 */
@Configuration
public class Knife4jConfig {

    /** client 前台接口分组 */
    @Bean(value = "clientDocket")
    public Docket clientDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .groupName("client前台接口")
                .select()
                // 仅扫描业务 Controller 包
                .apis(RequestHandlerSelectors.basePackage("com.greenchain.controller"))
                // 只暴露 /api/** 路径，内部工具接口（非 /api 前缀）不进文档
                .paths(PathSelectors.ant("/api/client/**"))
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    /** admin 后台接口分组 */
    @Bean(value = "adminDocket")
    public Docket adminDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .groupName("admin后台接口")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.greenchain.controller"))
                .paths(PathSelectors.ant("/api/admin/**"))
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("绿链锂电采购平台 API 文档")
                .description("client 前台 / admin 后台接口分组；鉴权方式：JWT，" +
                        "调试时在右上角 Authorize 填入 Bearer {token}")
                .contact(new Contact("green-chain", "", ""))
                .version("1.0")
                .build();
    }

    /**
     * 全局 Authorization 请求头：调试时自动在请求头附加 Authorization: {value}
     * <p>
     * 注意：knife4j 3.0.3 内置的 springfox 3.0.0 中，
     * Docket.securitySchemes 签名要求 List<SecurityScheme>，
     * 这里用反射创建 ApiKey 并上转型为 SecurityScheme，兼容不同版本 springfox 的类型边界。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SecurityScheme> securitySchemes() {
        try {
            Class<?> apiKeyClass = Class.forName("springfox.documentation.service.ApiKey");
            Object apiKey = apiKeyClass
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance("Authorization", "Authorization", "header");
            List list = new ArrayList<>();
            list.add((SecurityScheme) apiKey);
            return (List<SecurityScheme>) list;
        } catch (Exception e) {
            // fallback：无可用 ApiKey，文档不附加鉴权头（不影响接口本身运行）
            return Collections.emptyList();
        }
    }

    /**
     * 所有接口默认应用 Authorization 安全引用
     * <p>
     * 同样处理 springfox 不同版本 SecurityReference 构造方法签名差异：
     * 反射优先尝试双参构造（3.0.0），失败再尝试三参构造（2.9.x）。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SecurityContext> securityContexts() {
        try {
            Class<?> authScopeClass = Class.forName("springfox.documentation.service.AuthorizationScope");
            Class<?> secRefClass = Class.forName("springfox.documentation.service.SecurityReference");
            Object scope = authScopeClass
                    .getConstructor(String.class, String.class)
                    .newInstance("global", "全局访问token");
            Object[] scopesArray = (Object[]) java.lang.reflect.Array.newInstance(authScopeClass, 1);
            scopesArray[0] = scope;

            // 构造 SecurityReference
            Object secRef;
            try {
                secRef = secRefClass
                        .getConstructor(String.class, scopesArray.getClass())
                        .newInstance("Authorization", scopesArray);
            } catch (NoSuchMethodException ex) {
                // 2.9.x 老签名：三参
                secRef = secRefClass
                        .getConstructor(String.class, scopesArray.getClass(), Integer.TYPE)
                        .newInstance("Authorization", scopesArray, 0);
            }

            List<SecurityReference> refList = new ArrayList<>();
            refList.add((SecurityReference) secRef);

            return Collections.singletonList(
                    SecurityContext.builder()
                            .securityReferences(refList)
                            .forPaths(PathSelectors.regex("/.*"))
                            .build());
        } catch (Exception e) {
            // fallback：无安全上下文
            return Collections.emptyList();
        }
    }
}
