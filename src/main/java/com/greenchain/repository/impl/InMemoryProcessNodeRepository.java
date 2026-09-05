package com.greenchain.repository.impl;

import com.greenchain.entity.ProcessNode;
import com.greenchain.repository.ProcessNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InMemoryProcessNodeRepository implements ProcessNodeRepository {

    private final Map<String, ProcessNode> nodeStore = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        log.info("========== 初始化工序节点数据 ==========");
        ProcessNode p1 = new ProcessNode("Cutting", "极片切割", "cutting", "将正负极片切割成所需尺寸", "冲压车间", 2);
        ProcessNode p2 = new ProcessNode("Winding", "卷绕", "winding", "将正负极片与隔膜卷绕成电芯", "卷绕车间", 3);
        ProcessNode p3 = new ProcessNode("Formation", "化成", "formation", "首次充放电化成电芯", "化成车间", 8);
        ProcessNode p4 = new ProcessNode("Aging", "老化", "aging", "高温老化测试", "老化车间", 72);
        ProcessNode p5 = new ProcessNode("Packaging", "PACK封装", "packaging", "将电芯组装成电池模组", "封装车间", 4);
        ProcessNode p6 = new ProcessNode("Testing", "检测", "testing", "成品性能检测", "检测车间", 2);

        p1.setNextSteps(new ArrayList<>(Collections.singletonList(p2)));
        p2.setNextSteps(new ArrayList<>(Collections.singletonList(p3)));
        p3.setNextSteps(new ArrayList<>(Collections.singletonList(p4)));
        p4.setNextSteps(new ArrayList<>(Collections.singletonList(p5)));
        p5.setNextSteps(new ArrayList<>(Collections.singletonList(p6)));

        nodeStore.put(p1.getId(), p1);
        nodeStore.put(p2.getId(), p2);
        nodeStore.put(p3.getId(), p3);
        nodeStore.put(p4.getId(), p4);
        nodeStore.put(p5.getId(), p5);
        nodeStore.put(p6.getId(), p6);
        
        log.info("工序节点初始化完成，共 {} 个节点", nodeStore.size());
        nodeStore.forEach((k, v) -> log.info("  节点: {} - {}", k, v.getName()));
    }

    @Override
    public Optional<ProcessNode> findById(String id) {
        return Optional.ofNullable(nodeStore.get(id));
    }

    @Override
    public List<ProcessNode> findAll() {
        return new ArrayList<>(nodeStore.values());
    }

    @Override
    public List<ProcessNode> findByWorkshop(String workshop) {
        return nodeStore.values().stream()
                .filter(n -> workshop.equals(n.getWorkshop()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessNode> findFullProcessChain() {
        List<ProcessNode> chain = new ArrayList<>();
        ProcessNode start = nodeStore.values().iterator().next();
        if (start == null) {
            return chain;
        }
        chain.add(start);
        ProcessNode current = start;
        while (current.getNextSteps() != null && !current.getNextSteps().isEmpty()) {
            current = current.getNextSteps().get(0);
            chain.add(current);
        }
        return chain;
    }

    @Override
    public List<ProcessNode> findSubsequentNodes(String id) {
        List<ProcessNode> result = new ArrayList<>();
        ProcessNode node = nodeStore.get(id);
        if (node == null) {
            return result;
        }
        ProcessNode current = node;
        while (current.getNextSteps() != null && !current.getNextSteps().isEmpty()) {
            current = current.getNextSteps().get(0);
            result.add(current);
        }
        return result;
    }

    @Override
    public long countNodes() {
        return nodeStore.size();
    }

    @Override
    public long countRelationships() {
        long count = 0;
        for (ProcessNode node : nodeStore.values()) {
            if (node.getNextSteps() != null) {
                count += node.getNextSteps().size();
            }
        }
        return count;
    }
}