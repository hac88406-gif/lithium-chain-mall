package com.greenchain.repository.impl;

import com.greenchain.entity.Neo4jProcessNode;
import com.greenchain.entity.ProcessNode;
import com.greenchain.repository.Neo4jProcessNodeRepository;
import com.greenchain.repository.ProcessNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工序仓储的 Neo4j 实现。
 *
 * <p>通过 {@code @Primary} 覆盖 {@link InMemoryProcessNodeRepository}，
 * 全链路统一从 Neo4j 读取真实 P001~P014 节点与 NEXT_STEP 关系数据。
 *
 * <p><b>类名刻意不以「Neo4jProcessNodeRepository + Impl」结尾：</b>
 * Spring Data Neo4j 会把「SDN 接口名 + Impl」的类识别为该 Repository 的自定义
 * fragment 实现去装配，而本类构造器又注入了 SDN 接口，会形成自我循环依赖，
 * 导致启动报 BeanCurrentlyInCreationException。
 *
 * <p><b>关系水合策略：</b>SDN 6 自定义 @Query 不会填充 @Relationship 属性，
 * 所以 findAll 走 SDN 内置查询（自动水合 1 层关系）；
 * findByBusinessId / findSubsequentNodes 等自定义查询的结果，
 * 由标量查询 findNextStepIds 单独补上 nextStepIds。
 */
@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class ProcessNodeNeo4jRepository implements ProcessNodeRepository {

    private final Neo4jProcessNodeRepository delegate;

    @Override
    public Optional<ProcessNode> findById(String id) {
        try {
            return delegate.findByBusinessId(id).map(this::toProcessNodeWithNextSteps);
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode findById({}) 失败: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<ProcessNode> findAll() {
        try {
            // SDN 内置 findAll 自动水合 1 层 NEXT_STEP 关系
            return delegate.findAll().stream()
                    .map(this::toProcessNode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode findAll 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProcessNode> findByWorkshop(String workshop) {
        try {
            // SDN 派生查询自动水合 1 层 NEXT_STEP 关系
            return delegate.findByWorkshop(workshop).stream()
                    .map(this::toProcessNode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode findByWorkshop({}) 失败: {}", workshop, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProcessNode> findFullProcessChain() {
        try {
            // 链路 = 全部节点按 id 升序（P001 -> P014），关系随内置 findAll 水合
            return delegate.findAll().stream()
                    .sorted(Comparator.comparing(Neo4jProcessNode::getId, Comparator.nullsLast(String::compareTo)))
                    .map(this::toProcessNode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode findFullProcessChain 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProcessNode> findSubsequentNodes(String id) {
        try {
            return delegate.findSubsequentNodes(id).stream()
                    .map(this::toProcessNodeWithNextSteps)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode findSubsequentNodes({}) 失败: {}", id, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public long countNodes() {
        try {
            return delegate.countNodes();
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode countNodes 失败: {}", e.getMessage());
            return 0L;
        }
    }

    @Override
    public long countRelationships() {
        try {
            return delegate.countRelationships();
        } catch (Exception e) {
            log.warn("Neo4j ProcessNode countRelationships 失败: {}", e.getMessage());
            return 0L;
        }
    }

    // ========== 转换：Neo4jProcessNode → ProcessNode ==========

    /**
     * 基础转换（不含关系）：自定义查询返回的实体没有水合 nextSteps。
     */
    private ProcessNode toProcessNode(Neo4jProcessNode src) {
        if (src == null) {
            return null;
        }
        ProcessNode tgt = new ProcessNode();
        tgt.setId(src.getId());
        tgt.setName(src.getName());
        tgt.setType(src.getType());
        tgt.setDescription(src.getDescription());
        tgt.setWorkshop(src.getWorkshop());
        tgt.setDuration(src.getDuration());

        if (src.getNextSteps() != null && !src.getNextSteps().isEmpty()) {
            // SDN 内置/派生查询已水合 nextSteps，浅拷贝下游节点
            List<ProcessNode> nexts = new ArrayList<>();
            for (Neo4jProcessNode n : src.getNextSteps()) {
                nexts.add(toProcessNode(n));
            }
            tgt.setNextSteps(nexts);
        } else {
            tgt.setNextSteps(new ArrayList<>());
        }
        return tgt;
    }

    /**
     * 转换 + 关系补充：适用于自定义 @Query 的结果——
     * nextSteps 未水合，通过标量查询 findNextStepIds 单独补齐下游业务 id。
     */
    private ProcessNode toProcessNodeWithNextSteps(Neo4jProcessNode src) {
        ProcessNode tgt = toProcessNode(src);
        if (tgt == null) {
            return null;
        }
        List<String> nextIds = delegate.findNextStepIds(src.getId());
        if (nextIds != null && !nextIds.isEmpty()) {
            List<ProcessNode> nexts = new ArrayList<>();
            for (String nid : nextIds) {
                // 只需 id：业务层 VO 仅取下游 id（nextStepIds），无需完整数据
                ProcessNode shallow = new ProcessNode();
                shallow.setId(nid);
                nexts.add(shallow);
            }
            tgt.setNextSteps(nexts);
        }
        return tgt;
    }
}
