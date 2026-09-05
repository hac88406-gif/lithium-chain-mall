package com.greenchain.service;

import com.greenchain.dto.response.ProcessNodeVO;

import java.util.List;

/**
 * 工序图谱服务接口（Neo4j）
 */
public interface ProcessGraphService {

    /**
     * 查询所有工序节点
     */
    List<ProcessNodeVO> listAllNodes();

    /**
     * 查询完整工序链路（从起始到末尾）
     */
    List<ProcessNodeVO> getFullChain();

    /**
     * 查询单个节点详情
     */
    ProcessNodeVO getById(String id);

    /**
     * 查询某个节点的所有后续节点
     */
    List<ProcessNodeVO> getSubsequentNodes(String id);

    /**
     * 获取图谱统计信息
     */
    java.util.Map<String, Object> getGraphInfo();

    // ===== 数字孪生图算法接口（Neo4j 原生 Cypher） =====

    /**
     * 图算法总览：KPI（吞吐/瓶颈/良率/平衡率/关键路径）+ 风险传播链 + 设备冲突 + 产品追溯清单
     */
    java.util.Map<String, Object> getInsightOverview();

    /**
     * 影响范围推演：某工序异常后下游受影响工序 + 受影响产品 + 风险率损失（多跳遍历）
     */
    java.util.Map<String, Object> getImpactAnalysis(String id);

    /**
     * 节点全维度详情：设备/风险/缺陷/产品四类关联（一次 OPTIONAL MATCH 聚合）
     */
    java.util.Map<String, Object> getNodeInsight(String id);
}