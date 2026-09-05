package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.response.ProcessNodeVO;
import com.greenchain.service.ProcessGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工序图谱控制器（客户端）
 * 基于Neo4j的工序流程查询
 */
@RestController
@RequestMapping("/api/client/process")
@RequiredArgsConstructor
public class ProcessGraphController {

    private final ProcessGraphService processGraphService;

    /**
     * 查询所有工序节点
     * GET /api/client/process/list
     */
    @GetMapping("/list")
    public Result<List<ProcessNodeVO>> listAll() {
        List<ProcessNodeVO> list = processGraphService.listAllNodes();
        return Result.success(list);
    }

    /**
     * 查询完整工序链路
     * GET /api/client/process/chain
     */
    @GetMapping("/chain")
    public Result<Map<String, Object>> getFullChain() {
        List<ProcessNodeVO> chain = processGraphService.getFullChain();
        
        // 组装节点数据
        List<Map<String, Object>> resultNodes = new ArrayList<>();
        // 组装连线数据
        List<Map<String, Object>> resultEdges = new ArrayList<>();
        
        for (ProcessNodeVO node : chain) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", node.getId());
            nodeData.put("name", node.getName());
            nodeData.put("type", node.getType());
            nodeData.put("description", node.getDescription());
            
            // 安全处理 workshop 字段，转换为基本信息
            if (node.getWorkshop() != null) {
                Map<String, Object> workshopMap = new HashMap<>();
                workshopMap.put("id", node.getWorkshop().getId());
                workshopMap.put("name", node.getWorkshop().getName());
                nodeData.put("workshop", workshopMap);
            } else {
                nodeData.put("workshop", null);
            }
            
            nodeData.put("duration", node.getDuration());
            nodeData.put("nextStepIds", node.getNextStepIds() != null ? node.getNextStepIds() : new ArrayList<>());
            resultNodes.add(nodeData);
            
            // 构建连线
            if (node.getNextStepIds() != null && !node.getNextStepIds().isEmpty()) {
                for (String nextId : node.getNextStepIds()) {
                    Map<String, Object> edgeData = new HashMap<>();
                    edgeData.put("source", node.getId());
                    edgeData.put("target", nextId);
                    edgeData.put("relationType", "NEXT_PROCESS");
                    resultEdges.add(edgeData);
                }
            }
        }
        
        // 封装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("nodes", resultNodes);
        data.put("edges", resultEdges);
        
        return Result.success(data);
    }

    /**
     * 查询单个工序节点详情
     * GET /api/client/process/{id}
     */
    @GetMapping("/{id}")
    public Result<ProcessNodeVO> getById(@PathVariable String id) {
        ProcessNodeVO vo = processGraphService.getById(id);
        return Result.success(vo);
    }

    /**
     * 查询某节点的所有后续工序
     * GET /api/client/process/{id}/next
     */
    @GetMapping("/{id}/next")
    public Result<List<ProcessNodeVO>> getNext(@PathVariable String id) {
        List<ProcessNodeVO> list = processGraphService.getSubsequentNodes(id);
        return Result.success(list);
    }

    /**
     * 获取图谱统计信息
     * GET /api/client/process/info
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo() {
        Map<String, Object> info = processGraphService.getGraphInfo();
        return Result.success(info);
    }

    // ===== 数字孪生图算法接口（Neo4j 原生 Cypher） =====

    /**
     * 图算法总览：KPI + 风险传播链 + 设备冲突 + 产品追溯清单
     * GET /api/client/process/insight/overview
     */
    @GetMapping("/insight/overview")
    public Result<Map<String, Object>> getInsightOverview() {
        return Result.success(processGraphService.getInsightOverview());
    }

    /**
     * 影响范围推演：某工序异常后下游受影响工序 + 受影响产品 + 风险率损失
     * GET /api/client/process/insight/impact/{id}
     */
    @GetMapping("/insight/impact/{id}")
    public Result<Map<String, Object>> getImpactAnalysis(@PathVariable String id) {
        return Result.success(processGraphService.getImpactAnalysis(id));
    }

    /**
     * 节点全维度详情：设备/风险/缺陷/产品四类关联
     * GET /api/client/process/insight/node/{id}
     */
    @GetMapping("/insight/node/{id}")
    public Result<Map<String, Object>> getNodeInsight(@PathVariable String id) {
        return Result.success(processGraphService.getNodeInsight(id));
    }
}