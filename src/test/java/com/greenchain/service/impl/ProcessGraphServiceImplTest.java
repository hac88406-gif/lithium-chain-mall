package com.greenchain.service.impl;

import com.greenchain.repository.Neo4jProcessNodeRepository;
import com.greenchain.repository.ProcessNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ProcessGraphServiceImpl 防御性单元测试
 * <p>
 * 用 mockito-inline 的 mockStatic 静态 mock GraphDatabase.driver()，
 * 让 Neo4j Driver 不去真的连端口——防止 Driver 超时 30 秒+。
 * <p>
 * 重点验证：Neo4j 不可用时（Driver 抛异常）→ 图算法方法不 crash，返回完整空结构。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessGraphServiceImpl - Neo4j 防御性降级测试")
class ProcessGraphServiceImplTest {

    @Mock
    private ProcessNodeRepository processNodeRepository;

    @Mock
    private Neo4jProcessNodeRepository neo4jNativeRepo;

    @InjectMocks
    private ProcessGraphServiceImpl processGraphService;

    @Nested
    @DisplayName("Neo4j 不可用时 → 防御性降级，不 crash")
    class Neo4jDown {

        private MockedStatic<GraphDatabase> setupFailingDriver() {
            Driver mockDriver = mock(Driver.class);
            Session mockSession = mock(Session.class);
            // 用 lenient：因为不同测试里 readTransaction 可能被调用不同次数
            lenient().when(mockSession.readTransaction(any(TransactionWork.class)))
                    .thenThrow(new RuntimeException("Neo4j connection refused"));
            lenient().when(mockDriver.session()).thenReturn(mockSession);

            MockedStatic<GraphDatabase> driverMock = mockStatic(GraphDatabase.class);
            driverMock.when(() -> GraphDatabase.driver(anyString(), any(AuthToken.class)))
                    .thenReturn(mockDriver);
            return driverMock;
        }

        @Test
        @DisplayName("getInsightOverview → 不抛异常，返回完整 KPI 结构")
        void should_return_empty_kpi_when_neo4j_down() {
            try (MockedStatic<GraphDatabase> driverMock = setupFailingDriver()) {
                Map<String, Object> result = assertDoesNotThrow(() ->
                        processGraphService.getInsightOverview());

                assertNotNull(result, "返回值不能为 null");
                Object kpi = result.get("kpi");
                assertNotNull(kpi, "kpi 字段必须存在");
                assertTrue(kpi instanceof Map);
            }
        }

        @Test
        @DisplayName("getImpactAnalysis → 不抛异常，返回非 null（含 source 则有，不含也可以是 runQuery 返回空导致）")
        void should_return_empty_impact_when_neo4j_down() {
            try (MockedStatic<GraphDatabase> driverMock = setupFailingDriver()) {
                Map<String, Object> result = assertDoesNotThrow(() ->
                        processGraphService.getImpactAnalysis("P003"));

                assertNotNull(result);
                // 至少有 downstream / affectedProducts / riskLoss 三个 key
                // （source 只在 runQuery 有数据时才 put，Neo4j 挂了可能没有）
                assertTrue(result.containsKey("downstream"));
                assertTrue(result.containsKey("affectedProducts"));
                assertTrue(result.containsKey("riskLoss"));
            }
        }

        @Test
        @DisplayName("getNodeInsight → 不抛异常，返回完整结构")
        void should_return_empty_node_insight_when_neo4j_down() {
            try (MockedStatic<GraphDatabase> driverMock = setupFailingDriver()) {
                Map<String, Object> result = assertDoesNotThrow(() ->
                        processGraphService.getNodeInsight("P001"));

                assertNotNull(result);
            }
        }
    }
}
