package com.greenchain.repository;

import com.greenchain.entity.Neo4jProcessNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j 数据访问接口（ProcessNode 图节点底层仓储）。
 * 业务层请使用 {@link ProcessNodeRepository} 的 Neo4j 实现类（Primary）；
 * 本接口仅被 {@link com.greenchain.repository.impl.ProcessNodeNeo4jRepository} 委托调用。
 *
 * <p><b>设计说明（踩坑记录）：</b>SDN 6 的自定义 @Query 返回映射实体时，
 * 不会自动水合 @Relationship 关系属性（无论 RETURN 路径、collect(邻居) 还是平铺三列都不行）。
 * 因此这里采用双保险策略：
 * <ol>
 *   <li>findAll() 使用 SDN 内置查询（自动带 1 层 NEXT_STEP 关系水合）</li>
 *   <li>自定义查询只返回单列节点，关系由标量查询 findNextStepIds 单独补充</li>
 * </ol>
 */
@Repository
public interface Neo4jProcessNodeRepository extends Neo4jRepository<Neo4jProcessNode, Long> {

    /**
     * 按业务主键 id（P001~P014）查询单个节点。
     * 注意：自定义查询不含关系水合，nextSteps 由包装层通过 findNextStepIds 补充。
     */
    @Query("MATCH (n:ProcessNode) WHERE n.id = $id RETURN n")
    Optional<Neo4jProcessNode> findByBusinessId(@Param("id") String id);

    /**
     * 按车间名查询节点（SDN 派生查询，自动带 1 层 nextSteps 关系水合）。
     */
    List<Neo4jProcessNode> findByWorkshop(String workshop);

    /**
     * 查询 id 节点的所有后续节点（沿 NEXT_STEP 下游链路，不包含自身）。
     * 自定义查询不含关系水合，nextSteps 由包装层补充。
     */
    @Query("MATCH (start:ProcessNode) WHERE start.id = $id " +
            "MATCH (start)-[:NEXT_STEP*1..]->(next:ProcessNode) " +
            "RETURN DISTINCT next ORDER BY next.id")
    List<Neo4jProcessNode> findSubsequentNodes(@Param("id") String id);

    /**
     * 查询 id 节点的直接下游节点业务 id 列表（标量查询，给自定义查询补关系用）。
     */
    @Query("MATCH (n:ProcessNode {id: $id})-[:NEXT_STEP]->(m:ProcessNode) RETURN m.id")
    List<String> findNextStepIds(@Param("id") String id);

    @Query("MATCH (n:ProcessNode) RETURN count(n)")
    long countNodes();

    @Query("MATCH ()-[r:NEXT_STEP]->() RETURN count(r)")
    long countRelationships();
}
