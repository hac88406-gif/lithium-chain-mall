package com.greenchain.benchmark;

import com.greenchain.mapper.BenchmarkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j vs MySQL 图查询性能压测工具（多场景递进版）。
 *
 * <p><b>三个递进场景：</b>
 * <ol>
 *   <li><b>浅遍历（MySQL 占优）：</b>单产品 4 跳 OPTIONAL MATCH，结果 14 行，MySQL 全在内存里</li>
 *   <li><b>批量遍历（MySQL 开始吃力）：</b>100 个产品 × 7 表 JOIN，结果 1400 行</li>
 *   <li><b>深度递归（Neo4j 主场）：</b>从 P001 递归沿 NEXT_STEP 走到 P014，MySQL 必须用递归 CTE</li>
 * </ol>
 *
 * <p>使用方式：注入本组件，调用 {@link #runAllBenchmarks()} 即可获取三个场景的完整对比报告。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphVsSqlBenchmark {

    private final BenchmarkMapper benchmarkMapper;

    @Value("${spring.neo4j.uri:bolt://localhost:7687}")
    private String neo4jUri;

    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String neo4jUsername;

    @Value("${spring.neo4j.authentication.password:neo4j}")
    private String neo4jPassword;

    // ==================== 压测参数（多场景下调小，总时长可控） ====================

    /** 预热次数 */
    private static final int WARMUP_ROUNDS = 30;

    /** 每个场景的正式执行次数 */
    private static final int BENCHMARK_ROUNDS = 200;

    /** 场景①目标产品 */
    private static final String S1_PRODUCT_ID = "PRD002";

    /** 场景②：100 个 benchmark 产品（PRD1001~PRD1100），保证两边 category='benchmark' 过滤对等 */
    private static final List<String> S2_PRODUCT_IDS;
    static {
        List<String> ids = new ArrayList<>(100);
        for (int i = 1001; i <= 1100; i++) ids.add("PRD" + String.format("%04d", i));
        S2_PRODUCT_IDS = List.copyOf(ids);
    }

    /** 场景③起始工序 */
    private static final String S3_PROCESS_ID = "P001";

    // ==================== Neo4j Cypher（每个场景对应一条） ====================

    /** 场景①：单产品 4 跳浅遍历 */
    private static final String CYPHER_S1 =
            "MATCH (pr:Product {id: $productId})-[:CONTAINS]->(p:ProcessNode) " +
            "OPTIONAL MATCH (p)-[:USES]->(e:Equipment) " +
            "OPTIONAL MATCH (p)-[hr:HAS_RISK]->(q:QualityRisk) " +
            "OPTIONAL MATCH (q)-[:LEADS_TO]->(d:Defect) " +
            "RETURN p.id AS pid, p.name AS pname, p.workshop AS workshop, p.duration AS duration, " +
            "collect(DISTINCT e.name) AS equipment, " +
            "collect(DISTINCT q.name) AS risks, " +
            "collect(DISTINCT d.name) AS defects " +
            "ORDER BY p.id";

    /** 场景②：100 benchmark 产品批量 4 跳遍历（两边 category='benchmark' 过滤对等） */
    private static final String CYPHER_S2 =
            "MATCH (pr:Product)-[:CONTAINS]->(p:ProcessNode) " +
            "WHERE pr.id IN $productIds AND pr.category = 'benchmark' " +
            "OPTIONAL MATCH (p)-[:USES]->(e:Equipment) " +
            "OPTIONAL MATCH (p)-[hr:HAS_RISK]->(q:QualityRisk) " +
            "OPTIONAL MATCH (q)-[:LEADS_TO]->(d:Defect) " +
            "RETURN pr.id AS product_id, pr.name AS product_name, " +
            "p.id AS pid, p.name AS pname, p.workshop AS workshop, p.duration AS duration, " +
            "collect(DISTINCT e.name) AS equipment, " +
            "collect(DISTINCT q.name) AS risks, " +
            "collect(DISTINCT d.name) AS defects " +
            "ORDER BY pr.id, p.id";

    /** 场景③：深度递归遍历——沿 NEXT_STEP * 递归到终点（13 跳） */
    private static final String CYPHER_S3 =
            "MATCH path = (start:ProcessNode {id: $processId})-[:NEXT_STEP*]->(p:ProcessNode) " +
            "OPTIONAL MATCH (p)-[:USES]->(e:Equipment) " +
            "OPTIONAL MATCH (p)-[hr:HAS_RISK]->(q:QualityRisk) " +
            "OPTIONAL MATCH (q)-[:LEADS_TO]->(d:Defect) " +
            "RETURN p.id AS pid, p.name AS pname, p.workshop AS workshop, p.duration AS duration, " +
            "length(path) AS depth, " +
            "collect(DISTINCT e.name) AS equipment, " +
            "collect(DISTINCT q.name) AS risks, " +
            "collect(DISTINCT d.name) AS defects " +
            "ORDER BY depth, p.id";

    /** 场景④：全量 benchmark 产品 4 跳遍历（重头戏，两边 category='benchmark' 全量扫） */
    private static final String CYPHER_S4 =
            "MATCH (pr:Product)-[:CONTAINS]->(p:ProcessNode) " +
            "WHERE pr.category = 'benchmark' " +
            "OPTIONAL MATCH (p)-[:USES]->(e:Equipment) " +
            "OPTIONAL MATCH (p)-[hr:HAS_RISK]->(q:QualityRisk) " +
            "OPTIONAL MATCH (q)-[:LEADS_TO]->(d:Defect) " +
            "RETURN pr.id AS product_id, pr.name AS product_name, " +
            "p.id AS pid, p.name AS pname, p.workshop AS workshop, p.duration AS duration, " +
            "collect(DISTINCT e.name) AS equipment, " +
            "collect(DISTINCT q.name) AS risks, " +
            "collect(DISTINCT d.name) AS defects " +
            "ORDER BY pr.id, p.id";

    // ==================== 对外入口：跑全部四个场景 ====================

    /**
     * 执行四个递进场景的完整压测并返回汇总报告。
     */
    public Map<String, Object> runAllBenchmarks() {
        Map<String, Object> fullReport = new LinkedHashMap<>();
        List<Map<String, Object>> scenarios = new ArrayList<>();

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Neo4j vs MySQL 四场景递进压测 开始");
        log.info("  每场景预热 {} 次，正式执行 {} 次", WARMUP_ROUNDS, BENCHMARK_ROUNDS);
        log.info("  MySQL 数据规模：product_process 56027 行，graph_product 4003 个");
        log.info("═══════════════════════════════════════════════════════════");

        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
             Session session = driver.session()) {

            // ====== 场景①：单产品 4 跳浅遍历 ======
            runScenario(scenarios, session, "场景①", "单产品 4跳浅遍历（14行，MySQL全内存）",
                    () -> benchmarkMapper.queryProductProcessFullChain(S1_PRODUCT_ID),
                    CYPHER_S1, mkP("productId", S1_PRODUCT_ID));

            // ====== 场景②：100 benchmark 产品批量遍历 ======
            runScenario(scenarios, session, "场景②",
                    String.format("100产品批量 4跳遍历（~1400行，两边 category='benchmark'）"),
                    () -> benchmarkMapper.queryMultiProductsFullChain(S2_PRODUCT_IDS),
                    CYPHER_S2, mkP("productIds", S2_PRODUCT_IDS));

            // ====== 场景③：深度递归遍历 ======
            runScenario(scenarios, session, "场景③", "深度递归 NEXT_STEP 13跳 + 4跳遍历",
                    () -> benchmarkMapper.queryDeepProcessTraversal(S3_PROCESS_ID),
                    CYPHER_S3, mkP("processId", S3_PROCESS_ID));

            // ====== 场景④：全量 benchmark 4 跳遍历（重头戏！MySQL 8表JOIN扫42000行） ======
            runScenario(scenarios, session, "场景④",
                    "全量benchmark产品 4跳遍历（MySQL扫3000产品×42000行 8表JOIN）",
                    () -> benchmarkMapper.queryAllProductsFullChain(),
                    CYPHER_S4, new HashMap<>());

        } catch (Exception e) {
            log.error("Neo4j 连接异常: {}", e.getMessage(), e);
        }

        fullReport.put("meta", buildMeta());
        fullReport.put("scenarios", scenarios);
        printAllReports(scenarios);

        return fullReport;
    }

    /** 兼容性保留：只跑场景① */
    public Map<String, Object> runBenchmark() {
        return runAllBenchmarks();
    }

    // ==================== 单场景执行逻辑 ====================

    /**
     * 执行单个场景的：预热 → MySQL 压测 → Neo4j 压测 → 组装对比结果。
     */
    private void runScenario(List<Map<String, Object>> out, Session neo4jSession,
                              String scenarioKey, String scenarioDesc,
                              java.util.function.Supplier<List<Map<String, Object>>> mysqlCall,
                              String cypher, Map<String, Object> cypherParams) {

        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  {}：{}", scenarioKey, scenarioDesc);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ---- 预热 ----
        log.info("  预热 {} 次 ...", WARMUP_ROUNDS);
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            try { mysqlCall.get(); } catch (Exception ignored) {}
            try { neo4jSession.run(cypher, cypherParams).consume(); } catch (Exception ignored) {}
        }

        // ---- MySQL ----
        log.info("  MySQL 压测 {} 次 ...", BENCHMARK_ROUNDS);
        BenchmarkResult mysqlRes = timeMany("MySQL", mysqlCall, BENCHMARK_ROUNDS);

        // ---- Neo4j ----
        log.info("  Neo4j 压测 {} 次 ...", BENCHMARK_ROUNDS);
        BenchmarkResult neo4jRes = timeManyNeo4j("Neo4j", neo4jSession, cypher, cypherParams, BENCHMARK_ROUNDS);

        // ---- 组装 ----
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("key", scenarioKey);
        scenario.put("desc", scenarioDesc);
        scenario.put("mysql", toMap(mysqlRes));
        scenario.put("neo4j", toMap(neo4jRes));
        scenario.put("comparison", comparisonMap(mysqlRes, neo4jRes));
        out.add(scenario);

        log.info("  ✅ {} 完成：平均 MySQL={}ms, Neo4j={}ms, {}",
                scenarioKey, nsToMs(mysqlRes.avgNs), nsToMs(neo4jRes.avgNs),
                speedupText(mysqlRes, neo4jRes));
    }

    /** 对一个 Java Supplier 计时 N 次 */
    private BenchmarkResult timeMany(String label, java.util.function.Supplier<List<Map<String, Object>>> fn, int rounds) {
        List<Long> durs = new ArrayList<>(rounds);
        int rows = 0;
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            List<Map<String, Object>> r = fn.get();
            long elapsed = System.nanoTime() - start;
            durs.add(elapsed);
            if (i == 0 && r != null) rows = r.size();
        }
        return buildResult(label, durs, rows);
    }

    /** 对 Neo4j Session.run 计时 N 次 */
    private BenchmarkResult timeManyNeo4j(String label, Session session, String cypher,
                                          Map<String, Object> params, int rounds) {
        List<Long> durs = new ArrayList<>(rounds);
        int rows = 0;
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            List<Record> rs = session.run(cypher, params).list();
            long elapsed = System.nanoTime() - start;
            durs.add(elapsed);
            if (i == 0) rows = rs.size();
        }
        return buildResult(label, durs, rows);
    }

    // ==================== 工具方法 ====================

    private Map<String, Object> mkP(String k, Object v) {
        Map<String, Object> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    private Map<String, Object> buildMeta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("benchmark", "Neo4j vs MySQL 多场景递进压测");
        m.put("mysqlRows_product_process", 14027);
        m.put("mysqlRows_graph_product", 1003);
        m.put("warmupRounds", WARMUP_ROUNDS);
        m.put("benchmarkRounds", BENCHMARK_ROUNDS);
        m.put("timestamp", new Date().toString());
        return m;
    }

    private BenchmarkResult buildResult(String name, List<Long> durationsNs, int rowCount) {
        int n = durationsNs.size();
        if (n == 0) return new BenchmarkResult(name, 0, 0, 0, 0, 0, 0, rowCount);

        List<Long> sorted = new ArrayList<>(durationsNs);
        Collections.sort(sorted);
        long total = sorted.stream().mapToLong(Long::longValue).sum();

        return new BenchmarkResult(name,
                (double) total / n,
                sorted.get((int) (n * 0.5)),
                sorted.get((int) (n * 0.95)),
                sorted.get(0), sorted.get(n - 1),
                total, rowCount);
    }

    private Map<String, Object> toMap(BenchmarkResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("engine", r.name);
        m.put("avgMs", nsToMs(r.avgNs));
        m.put("p50Ms", nsToMs(r.p50Ns));
        m.put("p95Ms", nsToMs(r.p95Ns));
        m.put("minMs", nsToMs(r.minNs));
        m.put("maxMs", nsToMs(r.maxNs));
        m.put("totalMs", nsToMs(r.totalNs));
        m.put("rows", r.rowCount);
        m.put("rounds", BENCHMARK_ROUNDS);
        return m;
    }

    private Map<String, Object> comparisonMap(BenchmarkResult mysql, BenchmarkResult neo4j) {
        Map<String, Object> m = new LinkedHashMap<>();
        // ratio(慢, 快) = 慢的耗时 / 快的耗时，永远 >= 1，直接表示"快几倍"
        String faster = mysql.avgNs < neo4j.avgNs ? "MySQL" : "Neo4j";
        BenchmarkResult fasterRes = faster.equals("MySQL") ? mysql : neo4j;
        BenchmarkResult slowerRes = faster.equals("MySQL") ? neo4j : mysql;
        m.put("fasterEngine", faster);
        m.put("avgSpeedup",    ratio(slowerRes.avgNs, fasterRes.avgNs) + "x");
        m.put("p50Speedup",   ratio(slowerRes.p50Ns, fasterRes.p50Ns) + "x");
        m.put("p95Speedup",   ratio(slowerRes.p95Ns, fasterRes.p95Ns) + "x");
        m.put("totalTimeRatio", ratio(slowerRes.totalNs, fasterRes.totalNs) + "x");
        return m;
    }

    private String speedupText(BenchmarkResult mysql, BenchmarkResult neo4j) {
        String faster = mysql.avgNs < neo4j.avgNs ? "MySQL" : "Neo4j";
        BenchmarkResult fasterRes = faster.equals("MySQL") ? mysql : neo4j;
        BenchmarkResult slowerRes = faster.equals("MySQL") ? neo4j : mysql;
        double r = ratio(slowerRes.avgNs, fasterRes.avgNs);
        return String.format("%s 快 %.2fx", faster, r);
    }

    /** ratio(a,b) = a/b，即 a 是 b 的几倍 */
    private static double ratio(double a, double b) {
        if (b == 0) return Double.POSITIVE_INFINITY;
        return Math.round(a / b * 100.0) / 100.0;
    }

    private static double nsToMs(double ns) {
        return Math.round(ns / 1_000_000.0 * 100.0) / 100.0;
    }

    private static double nsToMs(long ns) {
        return Math.round(ns / 1_000_000.0 * 100.0) / 100.0;
    }

    // ==================== 控制台打印 ====================

    private void printAllReports(List<Map<String, Object>> scenarios) {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║  🏁  Neo4j vs MySQL 多场景递进压测 · 最终报告                               ║");
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");

        for (Map<String, Object> sc : scenarios) {
            @SuppressWarnings("unchecked") Map<String, Object> mysql = (Map<String, Object>) sc.get("mysql");
            @SuppressWarnings("unchecked") Map<String, Object> neo  = (Map<String, Object>) sc.get("neo4j");
            @SuppressWarnings("unchecked") Map<String, Object> comp = (Map<String, Object>) sc.get("comparison");

            log.info("");
            log.info("── {}：{} ────────────────────────────────────────", sc.get("key"), sc.get("desc"));
            // 用 Java String.format 做列对齐，SLF4J 只负责打印最终字符串
            log.info(String.format("  %-22s %12s  %12s", "指标", "MySQL", "Neo4j"));
            log.info("  ────────────────────────────────────────────────");
            log.info(String.format("  %-22s %10sms  %10sms", "平均耗时",  fmt(mysql.get("avgMs")),   fmt(neo.get("avgMs"))));
            log.info(String.format("  %-22s %10sms  %10sms", "P50 中位数",fmt(mysql.get("p50Ms")),  fmt(neo.get("p50Ms"))));
            log.info(String.format("  %-22s %10sms  %10sms", "P95 长尾",  fmt(mysql.get("p95Ms")),  fmt(neo.get("p95Ms"))));
            log.info(String.format("  %-22s %10sms  %10sms", "总耗时",    fmt(mysql.get("totalMs")),fmt(neo.get("totalMs"))));
            log.info(String.format("  %-22s %11s行  %11s行", "返回行数", fmt(mysql.get("rows")),   fmt(neo.get("rows"))));
            log.info("  🏆 胜者：{}（平均 {}，P50 {}，P95 {}）",
                    comp.get("fasterEngine"), comp.get("avgSpeedup"),
                    comp.get("p50Speedup"), comp.get("p95Speedup"));
        }
        log.info("");
    }

    private static String fmt(Object o) { return String.valueOf(o); }

    // ==================== 内部 DTO ====================

    private static class BenchmarkResult {
        final String name;
        final double avgNs;
        final double p50Ns;
        final double p95Ns;
        final long   minNs;
        final long   maxNs;
        final long   totalNs;
        final int    rowCount;

        BenchmarkResult(String name, double avgNs, double p50Ns, double p95Ns,
                        long minNs, long maxNs, long totalNs, int rowCount) {
            this.name = name; this.avgNs = avgNs; this.p50Ns = p50Ns; this.p95Ns = p95Ns;
            this.minNs = minNs; this.maxNs = maxNs; this.totalNs = totalNs; this.rowCount = rowCount;
        }
    }
}
