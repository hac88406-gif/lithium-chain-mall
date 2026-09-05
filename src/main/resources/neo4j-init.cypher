MATCH (n)-[r]->(m) DELETE r;
MATCH (n) DELETE n;

CREATE (n1:ProcessNode {id: 'P001', name: '原料预处理', type: 'PREPROCESS', description: '正负极材料的混合、研磨、干燥等预处理工序', workshop: '原料预处理车间', duration: 4});
CREATE (n2:ProcessNode {id: 'P002', name: '涂布', type: 'COATING', description: '将浆料均匀涂布在金属集流体上', workshop: '涂布车间', duration: 2});
CREATE (n3:ProcessNode {id: 'P003', name: '辊压', type: 'ROLLING', description: '对涂布后的极片进行辊压，提高能量密度', workshop: '涂布车间', duration: 1});
CREATE (n4:ProcessNode {id: 'P004', name: '分切', type: 'SLITTING', description: '将极片分切成所需宽度的极片', workshop: '涂布车间', duration: 1});
CREATE (n5:ProcessNode {id: 'P005', name: '卷绕', type: 'WINDING', description: '将正负极片与隔膜一起卷绕成电芯', workshop: '电芯卷绕车间', duration: 3});
CREATE (n6:ProcessNode {id: 'P006', name: '入壳', type: 'ENCASING', description: '将卷绕好的电芯装入外壳', workshop: '电芯卷绕车间', duration: 1});
CREATE (n7:ProcessNode {id: 'P007', name: '注液', type: 'INJECTION', description: '向电芯内注入电解液', workshop: '电芯卷绕车间', duration: 2});
CREATE (n8:ProcessNode {id: 'P008', name: '化成', type: 'FORMATION', description: '首次充放电化成，形成SEI膜', workshop: '化成车间', duration: 8});
CREATE (n9:ProcessNode {id: 'P009', name: '分容', type: 'CAPACITY_TEST', description: '对电芯进行容量测试与分组', workshop: '化成车间', duration: 6});
CREATE (n10:ProcessNode {id: 'P010', name: '焊接', type: 'WELDING', description: '电芯极耳焊接与极柱焊接', workshop: 'PACK封装车间', duration: 2});
CREATE (n11:ProcessNode {id: 'P011', name: '模组组装', type: 'MODULE_ASSEMBLY', description: '将电芯组装成电池模组', workshop: 'PACK封装车间', duration: 4});
CREATE (n12:ProcessNode {id: 'P012', name: '电池包封装', type: 'PACKAGING', description: '将模组封装成完整电池包', workshop: 'PACK封装车间', duration: 3});
CREATE (n13:ProcessNode {id: 'P013', name: '性能检测', type: 'PERFORMANCE_TEST', description: '电池包性能测试（容量、循环、安全等）', workshop: '质量检测车间', duration: 12});
CREATE (n14:ProcessNode {id: 'P014', name: '出厂检验', type: 'FINAL_INSPECTION', description: '最终检验与出厂前检测', workshop: '质量检测车间', duration: 2});

MATCH (a:ProcessNode {id: 'P001'}), (b:ProcessNode {id: 'P002'}) CREATE (a)-[:NEXT_STEP {order: 1}]->(b);
MATCH (a:ProcessNode {id: 'P002'}), (b:ProcessNode {id: 'P003'}) CREATE (a)-[:NEXT_STEP {order: 2}]->(b);
MATCH (a:ProcessNode {id: 'P003'}), (b:ProcessNode {id: 'P004'}) CREATE (a)-[:NEXT_STEP {order: 3}]->(b);
MATCH (a:ProcessNode {id: 'P004'}), (b:ProcessNode {id: 'P005'}) CREATE (a)-[:NEXT_STEP {order: 4}]->(b);
MATCH (a:ProcessNode {id: 'P005'}), (b:ProcessNode {id: 'P006'}) CREATE (a)-[:NEXT_STEP {order: 5}]->(b);
MATCH (a:ProcessNode {id: 'P006'}), (b:ProcessNode {id: 'P007'}) CREATE (a)-[:NEXT_STEP {order: 6}]->(b);
MATCH (a:ProcessNode {id: 'P007'}), (b:ProcessNode {id: 'P008'}) CREATE (a)-[:NEXT_STEP {order: 7}]->(b);
MATCH (a:ProcessNode {id: 'P008'}), (b:ProcessNode {id: 'P009'}) CREATE (a)-[:NEXT_STEP {order: 8}]->(b);
MATCH (a:ProcessNode {id: 'P009'}), (b:ProcessNode {id: 'P010'}) CREATE (a)-[:NEXT_STEP {order: 9}]->(b);
MATCH (a:ProcessNode {id: 'P010'}), (b:ProcessNode {id: 'P011'}) CREATE (a)-[:NEXT_STEP {order: 10}]->(b);
MATCH (a:ProcessNode {id: 'P011'}), (b:ProcessNode {id: 'P012'}) CREATE (a)-[:NEXT_STEP {order: 11}]->(b);
MATCH (a:ProcessNode {id: 'P012'}), (b:ProcessNode {id: 'P013'}) CREATE (a)-[:NEXT_STEP {order: 12}]->(b);
MATCH (a:ProcessNode {id: 'P013'}), (b:ProcessNode {id: 'P014'}) CREATE (a)-[:NEXT_STEP {order: 13}]->(b);

-- ============================================================
-- 以下为「数字孪生」扩展数据模型（Equipment / QualityRisk / Defect / Product）
-- 用于体现 Neo4j 多实体、多关系图查询优势（详见 ProcessGraphServiceImpl.initExtendedData）
-- ============================================================

-- ⚙️ 设备节点（Equipment）
CREATE (eq1:Equipment {id: 'E001', name: '高速双面涂布机 CL-03', model: 'COATER-CL03', oee: 0.92});
CREATE (eq2:Equipment {id: 'E002', name: '狭缝挤压涂布机 CL-05', model: 'COATER-CL05', oee: 0.85});
CREATE (eq3:Equipment {id: 'E003', name: 'X-Ray 面密度检测仪 QC-01', model: 'XRAY-QC01', oee: 0.88});
CREATE (eq4:Equipment {id: 'E004', name: '高速卷绕机 W-12', model: 'WINDER-W12', oee: 0.90});
CREATE (eq5:Equipment {id: 'E005', name: '全自动注液机 INJ-07', model: 'INJECT-INJ07', oee: 0.87});
CREATE (eq6:Equipment {id: 'E006', name: '化成充放电柜 FC-30', model: 'FORMATION-FC30', oee: 0.83});
CREATE (eq7:Equipment {id: 'E007', name: '激光焊接工作站 LW-02', model: 'LASER-LW02', oee: 0.91});
CREATE (eq8:Equipment {id: 'E008', name: 'PACK 终检测试台 EOL-05', model: 'EOL-EOL05', oee: 0.89});

-- 工序 USES 设备（注意：E003/E006/E007/E008 被多道工序共用 → 设备冲突分析数据源）
MATCH (p:ProcessNode {id: 'P002'}), (e:Equipment {id: 'E001'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P002'}), (e:Equipment {id: 'E002'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P002'}), (e:Equipment {id: 'E003'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P004'}), (e:Equipment {id: 'E003'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P005'}), (e:Equipment {id: 'E004'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P007'}), (e:Equipment {id: 'E005'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P008'}), (e:Equipment {id: 'E006'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P009'}), (e:Equipment {id: 'E006'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P010'}), (e:Equipment {id: 'E007'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P011'}), (e:Equipment {id: 'E007'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P013'}), (e:Equipment {id: 'E008'}) CREATE (p)-[:USES]->(e);
MATCH (p:ProcessNode {id: 'P014'}), (e:Equipment {id: 'E008'}) CREATE (p)-[:USES]->(e);

-- 🛡 质量风险节点（QualityRisk）
CREATE (r1:QualityRisk {id: 'R001', name: '极片厚度偏差'});
CREATE (r2:QualityRisk {id: 'R002', name: '浆料团聚颗粒'});
CREATE (r3:QualityRisk {id: 'R003', name: '卷绕对齐度超差'});
CREATE (r4:QualityRisk {id: 'R004', name: '电解液注液量偏差'});
CREATE (r5:QualityRisk {id: 'R005', name: 'SEI膜形成异常'});
CREATE (r6:QualityRisk {id: 'R006', name: '焊接虚焊/假焊'});
CREATE (r7:QualityRisk {id: 'R007', name: '容量一致性差'});

-- 工序 HAS_RISK 风险（rate = 发生率，level = 等级）
MATCH (p:ProcessNode {id: 'P002'}), (r:QualityRisk {id: 'R001'}) CREATE (p)-[:HAS_RISK {rate: 0.012, level: 'MEDIUM'}]->(r);
MATCH (p:ProcessNode {id: 'P002'}), (r:QualityRisk {id: 'R002'}) CREATE (p)-[:HAS_RISK {rate: 0.007, level: 'MEDIUM'}]->(r);
MATCH (p:ProcessNode {id: 'P005'}), (r:QualityRisk {id: 'R003'}) CREATE (p)-[:HAS_RISK {rate: 0.009, level: 'HIGH'}]->(r);
MATCH (p:ProcessNode {id: 'P007'}), (r:QualityRisk {id: 'R004'}) CREATE (p)-[:HAS_RISK {rate: 0.005, level: 'MEDIUM'}]->(r);
MATCH (p:ProcessNode {id: 'P008'}), (r:QualityRisk {id: 'R005'}) CREATE (p)-[:HAS_RISK {rate: 0.008, level: 'HIGH'}]->(r);
MATCH (p:ProcessNode {id: 'P010'}), (r:QualityRisk {id: 'R006'}) CREATE (p)-[:HAS_RISK {rate: 0.011, level: 'HIGH'}]->(r);
MATCH (p:ProcessNode {id: 'P009'}), (r:QualityRisk {id: 'R007'}) CREATE (p)-[:HAS_RISK {rate: 0.006, level: 'MEDIUM'}]->(r);

-- ⚠️ 缺陷节点（Defect）
CREATE (d1:Defect {id: 'D001', name: '容量不足'});
CREATE (d2:Defect {id: 'D002', name: '内阻超标'});
CREATE (d3:Defect {id: 'D003', name: '自放电偏大'});
CREATE (d4:Defect {id: 'D004', name: '循环寿命衰减快'});
CREATE (d5:Defect {id: 'D005', name: '鼓壳/变形'});
CREATE (d6:Defect {id: 'D006', name: '局部微短路'});

-- 风险 LEADS_TO 缺陷（风险传播链数据源）
MATCH (r:QualityRisk {id: 'R001'}), (d:Defect {id: 'D002'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R001'}), (d:Defect {id: 'D004'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R002'}), (d:Defect {id: 'D001'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R003'}), (d:Defect {id: 'D006'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R004'}), (d:Defect {id: 'D001'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R004'}), (d:Defect {id: 'D003'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R005'}), (d:Defect {id: 'D004'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R005'}), (d:Defect {id: 'D003'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R006'}), (d:Defect {id: 'D002'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R006'}), (d:Defect {id: 'D005'}) CREATE (r)-[:LEADS_TO]->(d);
MATCH (r:QualityRisk {id: 'R007'}), (d:Defect {id: 'D001'}) CREATE (r)-[:LEADS_TO]->(d);

-- 📦 产品节点（Product，对应商城真实商品品类）
CREATE (pr1:Product {id: 'PRD001', name: '21700圆柱电芯', category: '锂电池'});
CREATE (pr2:Product {id: 'PRD002', name: '家用储能PACK 10kWh', category: '储能电池'});
CREATE (pr3:Product {id: 'PRD003', name: '动力电池包 NCM 62kWh', category: '动力电池'});
CREATE (pr4:Product {id: 'PRD004', name: '便携储能电源（外购电芯）', category: '储能电池'});

-- 产品 CONTAINS 工序（产品工艺追溯数据源；PRD004 外购电芯只走 PACK 段）
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P001'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P002'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P003'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P004'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P005'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P006'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P007'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P008'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD001'}), (p:ProcessNode {id: 'P009'}) CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD002'}), (p:ProcessNode) WHERE p.id IN ['P001','P002','P003','P004','P005','P006','P007','P008','P009','P010','P011','P012','P013','P014'] CREATE (pr)-[:CONTAINS]->(p);
MATCH (pr:Product {id: 'PRD003'}), (p:ProcessNode) WHERE p.id IN ['P001','P002','P003','P004','P005','P006','P007','P008','P009','P010','P012','P013','P014'] CREATE (pr)-[:CONTAINS]->(p); -- PRD003 采用 CTP 无模组工艺，跳过 P011 模组组装
MATCH (pr:Product {id: 'PRD004'}), (p:ProcessNode) WHERE p.id IN ['P010','P011','P012','P013','P014'] CREATE (pr)-[:CONTAINS]->(p);