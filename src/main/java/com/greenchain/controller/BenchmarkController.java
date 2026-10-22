package com.greenchain.controller;

import com.greenchain.benchmark.GraphVsSqlBenchmark;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Neo4j vs MySQL 图查询性能压测接口。
 *
 * <p>直接访问 POST /api/benchmark/run 即可触发压测，
 * 返回格式化 JSON 对比报告，同时控制台也会打印人类可读的报告。
 *
 * <p>压测耗时约 5~15 秒（每个场景预热 30 次 + 正式执行 200 次），
 * 请耐心等待接口返回。
 *
 * <p>路径 /api/benchmark/** 不在 clientAuthInterceptor / adminAuditInterceptor 拦截范围内，
 * 无需登录即可访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final GraphVsSqlBenchmark benchmark;

    /**
     * 执行 Neo4j vs MySQL 图查询性能对比压测。
     *
     * <p>返回结构示例：
     * <pre>
     * {
     *   "meta":      { 两边数据规模、预热/正式次数、时间戳 },
     *   "scenarios": [
     *     { "key": "场景①", "desc": "...",
     *       "mysql":      { engine, avgMs, p50Ms, p95Ms, minMs, maxMs, totalMs, rows, rounds },
     *       "neo4j":      { 同上 },
     *       "comparison": { fasterEngine, avgSpeedup, p50Speedup, p95Speedup, totalTimeRatio } }
     *   ]
     * }
     * </pre>
     *
     * @return 完整对比报告 Map（会被 Jackson 自动序列化为 JSON）
     */
    @PostMapping("/run")
    public Map<String, Object> runBenchmark() {
        log.info("收到压测请求，开始执行 GraphVsSqlBenchmark...");
        Map<String, Object> report = benchmark.runBenchmark();
        log.info("压测完成，返回结果");
        return report;
    }

    /**
     * 健康检查：快速验证压测组件是否可用（不执行实际压测）。
     *
     * @return OK
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "OK", "service", "Neo4j vs MySQL Benchmark");
    }
}
