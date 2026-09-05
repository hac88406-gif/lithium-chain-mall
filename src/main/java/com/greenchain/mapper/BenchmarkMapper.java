package com.greenchain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Neo4j vs MySQL 压测专用 Mapper。
 *
 * <p>提供与 Cypher 图查询语义等价的 MySQL 多表 JOIN 查询，
 * 用于 {@code GraphVsSqlBenchmark} 公平对比两种数据库的图遍历性能。
 *
 * <p><b>三个递进压测场景：</b>
 * <ol>
 *   <li>单产品浅遍历（4 跳 JOIN，14 行结果）</li>
 *   <li>全产品批量遍历（7 表 JOIN + WHERE product_id IN (...)，2000+ 行结果）</li>
 *   <li>深度工序链递归（递归 CTE + 7 表 JOIN）</li>
 * </ol>
 */
@Mapper
public interface BenchmarkMapper {

    /**
     * 场景①：单产品 4 跳浅遍历（原压测场景，MySQL 占优）。
     *
     * <p>给定一个产品，查出它经过的每道工序 + 工序使用的设备 +
     * 工序存在的质量风险 + 风险可能导致的缺陷。
     *
     * @param productId 产品 id，如 'PRD002'
     * @return 每行一个工序，equipment/risks/defects 为逗号分隔去重字符串
     */
    List<Map<String, Object>> queryProductProcessFullChain(@Param("productId") String productId);

    /**
     * 场景②：多产品批量 4 跳遍历（数据量大了，MySQL JOIN 开始吃紧）。
     *
     * <p>查询一批产品的完整工艺链，等价 Cypher：
     * <pre>
     * MATCH (pr:Product)-[:CONTAINS]->(p:ProcessNode)
     * WHERE pr.id IN $productIds
     * OPTIONAL MATCH ...
     * RETURN pr.id, p.id, ...
     * </pre>
     *
     * @param productIds 产品 id 列表
     * @return 每行一个（产品, 工序）对
     */
    List<Map<String, Object>> queryMultiProductsFullChain(@Param("productIds") List<String> productIds);

    /**
     * 场景③：深度递归遍历——从一道工序出发，沿 NEXT_STEP 递归到终点，
     * 再叠加设备/风险/缺陷（MySQL 必须用递归 CTE，Neo4j 只需一条 ()-[:NEXT_STEP*]->()）。
     *
     * <p>Cypher 等价：
     * <pre>
     * MATCH (start:ProcessNode {id: $processId})-[:NEXT_STEP*]->(p:ProcessNode)
     * OPTIONAL MATCH (p)-[:USES]->(e:Equipment)
     * OPTIONAL MATCH (p)-[:HAS_RISK]->(q:QualityRisk)
     * OPTIONAL MATCH (q)-[:LEADS_TO]->(d:Defect)
     * RETURN p.id, collect(DISTINCT e.name), ...
     * </pre>
     *
     * @param processId 起始工序 id，如 'P001'
     * @return 递归链上每道工序的完整信息
     */
    List<Map<String, Object>> queryDeepProcessTraversal(@Param("processId") String processId);

    /**
     * 场景④：全量产品批量 4 跳遍历（无 WHERE 过滤，MySQL 8 表 JOIN 扫全表）。
     * MySQL 端要处理 graph_product 全表 JOIN product_process（56000+ 行），
     * Neo4j 端直接 MATCH 全图遍历。数据量越大差距越爆炸。
     *
     * <p>注：此场景是"全量压力"模式，两边数据量不等（MySQL 有 4000+ 产品，
     * Neo4j 有 1000+ 产品），重点看性能差距而非行数。
     */
    List<Map<String, Object>> queryAllProductsFullChain();
}
