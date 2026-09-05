package com.greenchain.service.impl;

import com.greenchain.common.BusinessException;
import com.greenchain.dto.response.ProcessNodeVO;
import com.greenchain.entity.Neo4jProcessNode;
import com.greenchain.entity.ProcessNode;
import com.greenchain.entity.Workshop;
import com.greenchain.repository.Neo4jProcessNodeRepository;
import com.greenchain.repository.ProcessNodeRepository;
import com.greenchain.service.ProcessGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工序图谱服务实现（全部统一走 Neo4j 真实数据，不再回退内存 6 条假节点）。
 *
 * <p>若 Neo4j 为空，会按 neo4j-init.cypher 定义的 14 条 P001~P014 节点自动 MERGE 初始化，
 * 形成 P001→P002→…→P014 的完整 NEXT_STEP 关系链路，与前端 Technology / ProcessGraph 渲染对齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessGraphServiceImpl implements ProcessGraphService {

    /** 统一业务仓储：@Primary 的 Neo4j 实现生效。 */
    private final ProcessNodeRepository processNodeRepository;

    /** SDN 原生仓储：仅保留注入兼容，不参与核心查询流程。 */
    @SuppressWarnings("unused")
    private final Neo4jProcessNodeRepository neo4jNativeRepo;

    @Value("${spring.neo4j.uri:bolt://localhost:7687}")
    private String neo4jUri;

    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String neo4jUsername;

    @Value("${spring.neo4j.authentication.password:neo4j}")
    private String neo4jPassword;

    @PostConstruct
    public void initNeo4j() {
        log.info("========== [工序图谱] 开始初始化 Neo4j 数据源 ==========");
        boolean connected = false;
        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword))) {
            driver.verifyConnectivity();
            connected = true;
            log.info("✅ Neo4j 连接成功: {}", neo4jUri);
        } catch (Exception e) {
            log.error("❌ Neo4j 连接失败：{} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("请先启动 Neo4j（Desktop2 DBMS dbms-f39c06ee...），否则工序相关接口返回空。");
        }

        if (!connected) {
            return;
        }

        long count = processNodeRepository.countNodes();
        if (count == 0) {
            log.info("⚠️ Neo4j ProcessNode 节点为空，按 P001~P014 标准数据初始化...");
            initNeo4jData();
            long after = processNodeRepository.countNodes();
            long relCount = processNodeRepository.countRelationships();
            log.info("✅ 初始化完成：节点 {} 个，关系 {} 条", after, relCount);
        } else {
            log.info("✅ 使用 Neo4j 现有工序数据：节点 {} 个，关系 {} 条",
                    count, processNodeRepository.countRelationships());
        }

        // 数字孪生扩展数据（Equipment/QualityRisk/Defect/Product 4 类节点 + USES/HAS_RISK/LEADS_TO/CONTAINS 4 类关系）
        long equipmentCount = countByCypher("MATCH (e:Equipment) RETURN count(e) AS c");
        if (equipmentCount == 0) {
            log.info("⚠️ Neo4j 扩展实体（Equipment 等）为空，初始化数字孪生数据模型...");
            initExtendedData();
        } else {
            log.info("✅ 使用 Neo4j 现有扩展实体：Equipment {} 台", equipmentCount);
        }

        // 数据修正（幂等，每次启动执行）：PRD003 动力电池包采用 CTP 无模组工艺，不包含 P011 模组组装
        runWrite("MATCH (:Product {id: 'PRD003'})-[c:CONTAINS]->(:ProcessNode {id: 'P011'}) DELETE c");
    }

    /** 通用写事务：执行单条写 Cypher（用于幂等数据修正）。 */
    private void runWrite(String cypher) {
        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
             Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                tx.run(cypher);
                return 1;
            });
        } catch (Exception e) {
            log.error("❌ Neo4j 数据修正失败：{} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** 通用只读 Cypher 查询：返回 List<Map>（Neo4j 值已转 Java 对象）。 */
    private List<Map<String, Object>> runQuery(String cypher, Map<String, Object> params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> p = params == null ? Collections.emptyMap() : params;
        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
             Session session = driver.session()) {
            session.readTransaction((TransactionWork<Integer>) tx -> {
                org.neo4j.driver.Result rs = tx.run(cypher, p);
                while (rs.hasNext()) {
                    Record rec = rs.next();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String k : rec.keys()) {
                        row.put(k, rec.get(k).asObject());
                    }
                    rows.add(row);
                }
                return rows.size();
            });
        } catch (Exception e) {
            log.error("❌ Neo4j 图算法查询失败：{} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return rows;
    }

    private long countByCypher(String cypher) {
        List<Map<String, Object>> rows = runQuery(cypher, null);
        if (rows.isEmpty()) {
            return 0;
        }
        Object v = rows.get(0).get("c");
        return v == null ? 0 : ((Number) v).longValue();
    }

    /**
     * 初始化数字孪生扩展数据：设备 / 质量风险 / 缺陷 / 产品 4 类节点
     * 及 USES / HAS_RISK / LEADS_TO / CONTAINS 4 类关系（对齐 neo4j-init.cypher 扩展段）。
     */
    private void initExtendedData() {
        // ⚙️ 设备（id/name/model/oee）
        List<Map<String, Object>> equipments = new ArrayList<>();
        equipments.add(props("E001", "高速双面涂布机 CL-03", "COATER-CL03", 0.92));
        equipments.add(props("E002", "狭缝挤压涂布机 CL-05", "COATER-CL05", 0.85));
        equipments.add(props("E003", "X-Ray 面密度检测仪 QC-01", "XRAY-QC01", 0.88));
        equipments.add(props("E004", "高速卷绕机 W-12", "WINDER-W12", 0.90));
        equipments.add(props("E005", "全自动注液机 INJ-07", "INJECT-INJ07", 0.87));
        equipments.add(props("E006", "化成充放电柜 FC-30", "FORMATION-FC30", 0.83));
        equipments.add(props("E007", "激光焊接工作站 LW-02", "LASER-LW02", 0.91));
        equipments.add(props("E008", "PACK 终检测试台 EOL-05", "EOL-EOL05", 0.89));

        // 工序 USES 设备（E003/E006/E007/E008 被多道工序共用 → 设备冲突分析数据源）
        String[][] uses = {
                {"P002", "E001"}, {"P002", "E002"}, {"P002", "E003"}, {"P004", "E003"},
                {"P005", "E004"}, {"P007", "E005"}, {"P008", "E006"}, {"P009", "E006"},
                {"P010", "E007"}, {"P011", "E007"}, {"P013", "E008"}, {"P014", "E008"}
        };

        // 🛡 质量风险
        String[][] risks = {
                {"R001", "极片厚度偏差"}, {"R002", "浆料团聚颗粒"}, {"R003", "卷绕对齐度超差"},
                {"R004", "电解液注液量偏差"}, {"R005", "SEI膜形成异常"}, {"R006", "焊接虚焊/假焊"},
                {"R007", "容量一致性差"}
        };
        // 工序 HAS_RISK 风险（rate=发生率 level=等级）
        String[][] hasRisk = {
                {"P002", "R001", "0.012", "MEDIUM"}, {"P002", "R002", "0.007", "MEDIUM"},
                {"P005", "R003", "0.009", "HIGH"}, {"P007", "R004", "0.005", "MEDIUM"},
                {"P008", "R005", "0.008", "HIGH"}, {"P010", "R006", "0.011", "HIGH"},
                {"P009", "R007", "0.006", "MEDIUM"}
        };
        // ⚠️ 缺陷
        String[][] defects = {
                {"D001", "容量不足"}, {"D002", "内阻超标"}, {"D003", "自放电偏大"},
                {"D004", "循环寿命衰减快"}, {"D005", "鼓壳/变形"}, {"D006", "局部微短路"}
        };
        // 风险 LEADS_TO 缺陷
        String[][] leadsTo = {
                {"R001", "D002"}, {"R001", "D004"}, {"R002", "D001"}, {"R003", "D006"},
                {"R004", "D001"}, {"R004", "D003"}, {"R005", "D004"}, {"R005", "D003"},
                {"R006", "D002"}, {"R006", "D005"}, {"R007", "D001"}
        };
        // 📦 产品
        String[][] products = {
                {"PRD001", "21700圆柱电芯", "锂电池"},
                {"PRD002", "家用储能PACK 10kWh", "储能电池"},
                {"PRD003", "动力电池包 NCM 62kWh", "动力电池"},
                {"PRD004", "便携储能电源（外购电芯）", "储能电池"}
        };
        // 产品 CONTAINS 工序（PRD001 只做电芯段 P001~P009；PRD003 采用 CTP 无模组工艺跳过 P011；
        // PRD004 外购电芯只走 PACK 段 P010~P014 → 使产品追溯能查出差异化子集）
        String[][] contains = {
                {"PRD001", "P001"}, {"PRD001", "P002"}, {"PRD001", "P003"}, {"PRD001", "P004"},
                {"PRD001", "P005"}, {"PRD001", "P006"}, {"PRD001", "P007"}, {"PRD001", "P008"},
                {"PRD001", "P009"},
                {"PRD002", "P001"}, {"PRD002", "P002"}, {"PRD002", "P003"}, {"PRD002", "P004"},
                {"PRD002", "P005"}, {"PRD002", "P006"}, {"PRD002", "P007"}, {"PRD002", "P008"},
                {"PRD002", "P009"}, {"PRD002", "P010"}, {"PRD002", "P011"}, {"PRD002", "P012"},
                {"PRD002", "P013"}, {"PRD002", "P014"},
                {"PRD003", "P001"}, {"PRD003", "P002"}, {"PRD003", "P003"}, {"PRD003", "P004"},
                {"PRD003", "P005"}, {"PRD003", "P006"}, {"PRD003", "P007"}, {"PRD003", "P008"},
                {"PRD003", "P009"}, {"PRD003", "P010"}, {"PRD003", "P012"},
                {"PRD003", "P013"}, {"PRD003", "P014"},
                {"PRD004", "P010"}, {"PRD004", "P011"}, {"PRD004", "P012"}, {"PRD004", "P013"},
                {"PRD004", "P014"}
        };

        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
             Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                for (Map<String, Object> e : equipments) {
                    tx.run("MERGE (n:Equipment {id: $id}) SET n.name = $name, n.model = $model, n.oee = $oee", e);
                }
                for (String[] u : uses) {
                    tx.run("MATCH (p:ProcessNode {id: $pid}), (e:Equipment {id: $eid}) MERGE (p)-[:USES]->(e)",
                            props2("pid", u[0], "eid", u[1]));
                }
                for (String[] r : risks) {
                    tx.run("MERGE (n:QualityRisk {id: $id}) SET n.name = $name",
                            props2("id", r[0], "name", r[1]));
                }
                for (String[] hr : hasRisk) {
                    tx.run("MATCH (p:ProcessNode {id: $pid}), (r:QualityRisk {id: $rid}) " +
                                    "MERGE (p)-[x:HAS_RISK]->(r) SET x.rate = toFloat($rate), x.level = $level",
                            props4("pid", hr[0], "rid", hr[1], "rate", hr[2], "level", hr[3]));
                }
                for (String[] d : defects) {
                    tx.run("MERGE (n:Defect {id: $id}) SET n.name = $name",
                            props2("id", d[0], "name", d[1]));
                }
                for (String[] lt : leadsTo) {
                    tx.run("MATCH (r:QualityRisk {id: $rid}), (d:Defect {id: $did}) MERGE (r)-[:LEADS_TO]->(d)",
                            props2("rid", lt[0], "did", lt[1]));
                }
                for (String[] pr : products) {
                    tx.run("MERGE (n:Product {id: $id}) SET n.name = $name, n.category = $category",
                            props3(pr[0], pr[1], pr[2]));
                }
                for (String[] c : contains) {
                    tx.run("MATCH (pr:Product {id: $prid}), (p:ProcessNode {id: $pid}) MERGE (pr)-[:CONTAINS]->(p)",
                            props2("prid", c[0], "pid", c[1]));
                }
                return 1;
            });
            log.info("✅ 数字孪生扩展数据初始化完成：设备 {} / 风险 {} / 缺陷 {} / 产品 {}",
                    equipments.size(), risks.length, defects.length, products.length);
        } catch (Exception e) {
            log.error("❌ Neo4j 扩展实体初始化失败：{} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static Map<String, Object> props(String id, String name, String model, double oee) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("model", model);
        m.put("oee", oee);
        return m;
    }

    private static Map<String, Object> props2(String k1, String v1, String k2, String v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Object> props3(String id, String name, String category) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("category", category);
        return m;
    }

    private static Map<String, Object> props4(String k1, String v1, String k2, String v2,
                                              String k3, String v3, String k4, String v4) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        m.put(k4, v4);
        return m;
    }

    /**
     * 初始化 14 条标准工序节点（对齐 src/main/resources/neo4j-init.cypher）。
     * 使用批量参数 Map，避免在 lambda/匿名类中捕获 Values 等外部引用，保持编译一致。
     */
    private void initNeo4jData() {
        // 14 节点：P001~P014
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(makeNode("P001","原料预处理","PREPROCESS","正负极材料的混合、研磨、干燥等预处理工序","原料预处理车间",4));
        nodes.add(makeNode("P002","涂布","COATING","将浆料均匀涂布在金属集流体上","涂布车间",2));
        nodes.add(makeNode("P003","辊压","ROLLING","对涂布后的极片进行辊压，提高能量密度","涂布车间",1));
        nodes.add(makeNode("P004","分切","SLITTING","将极片分切成所需宽度的极片","涂布车间",1));
        nodes.add(makeNode("P005","卷绕","WINDING","将正负极片与隔膜一起卷绕成电芯","电芯卷绕车间",3));
        nodes.add(makeNode("P006","入壳","ENCASING","将卷绕好的电芯装入外壳","电芯卷绕车间",1));
        nodes.add(makeNode("P007","注液","INJECTION","向电芯内注入电解液","电芯卷绕车间",2));
        nodes.add(makeNode("P008","化成","FORMATION","首次充放电化成，形成SEI膜","化成车间",8));
        nodes.add(makeNode("P009","分容","CAPACITY_TEST","对电芯进行容量测试与分组","化成车间",6));
        nodes.add(makeNode("P010","焊接","WELDING","电芯极耳焊接与极柱焊接","PACK封装车间",2));
        nodes.add(makeNode("P011","模组组装","MODULE_ASSEMBLY","将电芯组装成电池模组","PACK封装车间",4));
        nodes.add(makeNode("P012","电池包封装","PACKAGING","将模组封装成完整电池包","PACK封装车间",3));
        nodes.add(makeNode("P013","性能检测","PERFORMANCE_TEST","电池包性能测试（容量、循环、安全等）","质量检测车间",12));
        nodes.add(makeNode("P014","出厂检验","FINAL_INSPECTION","最终检验与出厂前检测","质量检测车间",2));

        // NEXT_STEP 关系（13 条 P001->P002 ... P013->P014）
        List<Map<String, Object>> edges = new ArrayList<>();
        edges.add(makeEdge("P001","P002",1));
        edges.add(makeEdge("P002","P003",2));
        edges.add(makeEdge("P003","P004",3));
        edges.add(makeEdge("P004","P005",4));
        edges.add(makeEdge("P005","P006",5));
        edges.add(makeEdge("P006","P007",6));
        edges.add(makeEdge("P007","P008",7));
        edges.add(makeEdge("P008","P009",8));
        edges.add(makeEdge("P009","P010",9));
        edges.add(makeEdge("P010","P011",10));
        edges.add(makeEdge("P011","P012",11));
        edges.add(makeEdge("P012","P013",12));
        edges.add(makeEdge("P013","P014",13));

        final String nodeCypher =
                "MERGE (n:ProcessNode {id: $id}) " +
                "SET n.name = $name, n.type = $type, n.description = $description, " +
                "    n.workshop = $workshop, n.duration = $duration";
        final String edgeCypher =
                "MATCH (a:ProcessNode {id: $from}), (b:ProcessNode {id: $to}) " +
                "MERGE (a)-[r:NEXT_STEP]->(b) SET r.order = $order";

        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
             Session session = driver.session()) {
            // 节点：一个 writeTransaction 中批量 MERGE（lambda 内直接引用 final 变量即可）
            final List<Map<String, Object>> fNodes = nodes;
            TransactionWork<Integer> nodeWork = tx -> {
                for (Map<String, Object> p : fNodes) {
                    tx.run(nodeCypher, new LinkedHashMap<>(p));
                }
                return fNodes.size();
            };
            session.writeTransaction(nodeWork);

            // 关系：一个 writeTransaction 中批量 MERGE
            final List<Map<String, Object>> fEdges = edges;
            TransactionWork<Integer> edgeWork = tx -> {
                for (Map<String, Object> e : fEdges) {
                    tx.run(edgeCypher, new LinkedHashMap<>(e));
                }
                return fEdges.size();
            };
            session.writeTransaction(edgeWork);
        } catch (Exception e) {
            log.error("❌ Neo4j 初始化 14 条工序节点失败：{} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static Map<String, Object> makeNode(String id, String name, String type, String desc,
                                                String workshop, int duration) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("type", type);
        m.put("description", desc);
        m.put("workshop", workshop);
        m.put("duration", duration);
        return m;
    }

    private static Map<String, Object> makeEdge(String from, String to, int order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", from);
        m.put("to", to);
        m.put("order", order);
        return m;
    }

    @Override
    public List<ProcessNodeVO> listAllNodes() {
        return processNodeRepository.findAll().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessNodeVO> getFullChain() {
        List<ProcessNode> chain = processNodeRepository.findFullProcessChain();
        if (chain == null || chain.isEmpty()) {
            chain = processNodeRepository.findAll();
        }
        return chain.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public ProcessNodeVO getById(String id) {
        return processNodeRepository.findById(id)
                .map(this::convertToVO)
                .orElseThrow(() -> new BusinessException(404, "工序节点不存在: " + id));
    }

    @Override
    public List<ProcessNodeVO> getSubsequentNodes(String id) {
        return processNodeRepository.findSubsequentNodes(id).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getGraphInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nodeCount", processNodeRepository.countNodes());
        info.put("relationshipCount", processNodeRepository.countRelationships());
        return info;
    }

    private ProcessNodeVO convertToVO(ProcessNode node) {
        ProcessNodeVO vo = new ProcessNodeVO();
        vo.setId(node.getId());
        vo.setName(node.getName());
        vo.setType(node.getType());
        vo.setDescription(node.getDescription());
        vo.setDuration(node.getDuration());

        if (node.getWorkshop() != null) {
            Workshop workshop = new Workshop();
            workshop.setName(node.getWorkshop());
            vo.setWorkshop(workshop);
        }

        if (node.getNextSteps() != null && !node.getNextSteps().isEmpty()) {
            List<String> nextIds = node.getNextSteps().stream()
                    .map(ProcessNode::getId)
                    .collect(Collectors.toList());
            vo.setNextStepIds(nextIds);
        }
        return vo;
    }

    /** @deprecated 历史代码冗余，删除双套转换逻辑。 */
    @Deprecated
    @SuppressWarnings("unused")
    private ProcessNodeVO convertNeo4jToVO(Neo4jProcessNode node) {
        return null;
    }

    // ====================================================================
    // 以下为「数字孪生图算法」接口（全部 Neo4j 原生 Cypher，体现图数据库优势）
    // ====================================================================

    /**
     * 图算法总览：KPI（总吞吐/瓶颈/良率/产线平衡率/关键路径）+ 风险传播链 + 设备冲突 + 产品追溯清单。
     */
    @Override
    public Map<String, Object> getInsightOverview() {
        Map<String, Object> data = new HashMap<>();

        // 1) 节点耗时统计（MySQL 需要 GROUP BY，这里一行 Cypher 全带走）
        List<Map<String, Object>> nodes = runQuery(
                "MATCH (n:ProcessNode) RETURN n.id AS id, n.name AS name, n.duration AS duration", null);
        long totalDuration = 0;
        String bottleneckName = null;
        long bottleneckDuration = 0;
        for (Map<String, Object> n : nodes) {
            long d = n.get("duration") == null ? 0 : ((Number) n.get("duration")).longValue();
            totalDuration += d;
            if (d > bottleneckDuration) {
                bottleneckDuration = d;
                bottleneckName = String.valueOf(n.get("name"));
            }
        }

        // 2) 关键路径（CPM 最长加权路径，Neo4j path + reduce 一行实现）
        List<Map<String, Object>> cp = runQuery(
                "MATCH p = (s:ProcessNode)-[:NEXT_STEP*]->(e:ProcessNode) " +
                        "WHERE NOT ()-[:NEXT_STEP]->(s) AND NOT (e)-[:NEXT_STEP]->() " +
                        "WITH p, reduce(t = 0, n IN nodes(p) | t + coalesce(n.duration, 0)) AS total " +
                        "RETURN [n IN nodes(p) | n.id] AS ids, total ORDER BY total DESC LIMIT 1", null);
        List<String> criticalPathIds = new ArrayList<>();
        long criticalPathTotal = 0;
        if (!cp.isEmpty()) {
            criticalPathIds.addAll((List<String>) cp.get(0).get("ids"));
            criticalPathTotal = cp.get(0).get("total") == null ? 0 : ((Number) cp.get(0).get("total")).longValue();
        }

        // 3) 模拟良率 = 所有风险点 (1 - rate) 连乘（沿 HAS_RISK 关系聚合）
        List<Map<String, Object>> rates = runQuery(
                "MATCH ()-[r:HAS_RISK]->() RETURN r.rate AS rate", null);
        double yield = 1.0;
        for (Map<String, Object> r : rates) {
            Object v = r.get("rate");
            if (v instanceof Number) {
                yield *= (1.0 - ((Number) v).doubleValue());
            }
        }

        // 4) 产线平衡率 = 总工时 / (节点数 × 瓶颈工时)
        double balance = nodes.isEmpty() || bottleneckDuration == 0
                ? 0 : (double) totalDuration / (nodes.size() * bottleneckDuration);

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("nodeCount", nodes.size());
        kpi.put("totalDuration", totalDuration);
        kpi.put("bottleneckName", bottleneckName);
        kpi.put("bottleneckDuration", bottleneckDuration);
        kpi.put("bottleneckShare", totalDuration == 0 ? 0
                : Math.round(bottleneckDuration * 1000.0 / totalDuration) / 10.0);
        kpi.put("yieldRate", Math.round(yield * 10000.0) / 100.0);
        kpi.put("balanceRate", Math.round(balance * 10000.0) / 100.0);
        kpi.put("criticalPathIds", criticalPathIds);
        kpi.put("criticalPathTotal", criticalPathTotal);
        data.put("kpi", kpi);

        // 5) 风险传播链：工序 -HAS_RISK-> 风险 -LEADS_TO-> 缺陷（3 跳多关系遍历）
        data.put("riskChains", runQuery(
                "MATCH (p:ProcessNode)-[hr:HAS_RISK]->(q:QualityRisk)-[:LEADS_TO]->(d:Defect) " +
                        "RETURN p.id AS processId, p.name AS processName, q.id AS riskId, q.name AS riskName, " +
                        "hr.rate AS rate, hr.level AS level, d.name AS defectName " +
                        "ORDER BY hr.rate DESC", null));

        // 6) 设备共用冲突：同一台设备被两道工序 USES
        data.put("equipmentConflicts", runQuery(
                "MATCH (p1:ProcessNode)-[:USES]->(e:Equipment)<-[:USES]-(p2:ProcessNode) " +
                        "WHERE p1.id < p2.id " +
                        "RETURN e.id AS equipmentId, e.name AS equipmentName, e.oee AS oee, " +
                        "p1.id AS p1Id, p1.name AS p1Name, p2.id AS p2Id, p2.name AS p2Name " +
                        "ORDER BY e.id", null));

        // 7) 产品工艺追溯清单（排除 benchmark 膨胀数据，避免前端污染）
        data.put("products", runQuery(
                "MATCH (pr:Product)-[:CONTAINS]->(p:ProcessNode) " +
                        "WHERE pr.category <> 'benchmark' " +
                        "RETURN pr.id AS productId, pr.name AS productName, pr.category AS category, " +
                        "collect(p.id) AS processIds", null));

        return data;
    }

    /**
     * 影响范围推演：某工序异常后，沿 NEXT_STEP 多跳遍历下游全部受影响工序 + 受影响产品 + 良率损失。
     */
    @Override
    public Map<String, Object> getImpactAnalysis(String id) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> params = props2("id", id, "dummy", "");

        // 下游工序（多跳遍历，Neo4j 核心优势）
        data.put("downstream", runQuery(
                "MATCH (s:ProcessNode {id: $id})-[:NEXT_STEP*1..]->(n:ProcessNode) " +
                        "RETURN DISTINCT n.id AS id, n.name AS name ORDER BY n.id", params));

        // 受影响产品（工序*0.. 多跳 + CONTAINS 反查，排除 benchmark）
        data.put("affectedProducts", runQuery(
                "MATCH (s:ProcessNode {id: $id})-[:NEXT_STEP*0..]->(n:ProcessNode)<-[:CONTAINS]-(pr:Product) " +
                        "WHERE pr.category <> 'benchmark' " +
                        "RETURN DISTINCT pr.id AS id, pr.name AS name, pr.category AS category ORDER BY pr.id", params));

        // 风险率损失（含自身，沿链路聚合 HAS_RISK rate）
        double loss = 0.0;
        List<Map<String, Object>> lossRows = runQuery(
                "MATCH (s:ProcessNode {id: $id})-[:NEXT_STEP*0..]->(n:ProcessNode)-[r:HAS_RISK]->() " +
                        "RETURN sum(coalesce(r.rate, 0)) AS loss", params);
        if (!lossRows.isEmpty() && lossRows.get(0).get("loss") instanceof Number) {
            loss = ((Number) lossRows.get(0).get("loss")).doubleValue();
        }
        data.put("riskLoss", Math.round(loss * 10000.0) / 100.0);

        // 起始工序本身
        List<Map<String, Object>> self = runQuery(
                "MATCH (n:ProcessNode {id: $id}) RETURN n.id AS id, n.name AS name", params);
        if (!self.isEmpty()) {
            data.put("source", self.get(0));
        }
        return data;
    }

    /**
     * 节点全维度详情：一次 Cypher 聚合出该工序的 设备/风险/缺陷/产品 四类关联（OPTIONAL MATCH + collect）。
     */
    @Override
    public Map<String, Object> getNodeInsight(String id) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> params = props2("id", id, "dummy", "");

        List<Map<String, Object>> rows = runQuery(
                "MATCH (p:ProcessNode {id: $id}) " +
                        "OPTIONAL MATCH (p)-[:USES]->(e:Equipment) " +
                        "OPTIONAL MATCH (p)-[hr:HAS_RISK]->(q:QualityRisk) " +
                        "OPTIONAL MATCH (q)-[:LEADS_TO]->(d:Defect) " +
                        "OPTIONAL MATCH (p)<-[:CONTAINS]-(pr:Product) WHERE pr.category <> 'benchmark' " +
                        "RETURN p.id AS id, p.name AS name, p.workshop AS workshop, p.duration AS duration, " +
                        "collect(DISTINCT e.name) AS equipment, " +
                        "collect(DISTINCT e.oee) AS equipmentOee, " +
                        "collect(DISTINCT q.name) AS riskNames, " +
                        "collect(DISTINCT coalesce(hr.rate, 0)) AS riskRates, " +
                        "collect(DISTINCT coalesce(hr.level, '')) AS riskLevels, " +
                        "collect(DISTINCT d.name) AS defects, " +
                        "collect(DISTINCT pr.name) AS products", params);

        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            data.put("id", r.get("id"));
            data.put("name", r.get("name"));
            data.put("workshop", r.get("workshop"));
            data.put("duration", r.get("duration"));
            data.put("equipment", r.get("equipment"));
            data.put("equipmentOee", r.get("equipmentOee"));
            data.put("riskNames", r.get("riskNames"));
            data.put("riskRates", r.get("riskRates"));
            data.put("riskLevels", r.get("riskLevels"));
            data.put("defects", r.get("defects"));
            data.put("products", r.get("products"));
        }
        return data;
    }
}
