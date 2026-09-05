package com.greenchain.controller;

import com.greenchain.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 * 提供项目基本信息和接口列表
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Result<Map<String, Object>> home() {
        Map<String, Object> info = new HashMap<>();
        info.put("project", "绿链锂电采购平台后端");
        info.put("version", "1.0.0");
        info.put("status", "运行中");
        info.put("server", "http://localhost:8080");

        Map<String, String> apis = new HashMap<>();
        apis.put("用户登录", "POST /api/client/user/login");
        apis.put("商品列表", "GET /api/client/product/page");
        apis.put("购物车列表", "GET /api/client/cart/list");
        apis.put("创建订单", "POST /api/client/order/create");
        apis.put("工序图谱", "GET /api/client/process/list");
        apis.put("车间列表", "GET /api/client/workshop/list");
        info.put("api列表", apis);

        return Result.success("绿链锂电采购平台启动成功", info);
    }
}