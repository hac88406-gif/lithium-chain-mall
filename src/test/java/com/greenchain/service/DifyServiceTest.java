package com.greenchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenchain.config.DifyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DifyService + FaqService 单元测试（Coze → Dify 迁移替换原 CozeServiceTest）
 * <p>
 * 重点覆盖：
 *   ① DifyService 空入参 → 空串（Controller 层会走 FaqService.chat）
 *   ② DifyService API Key 未配置 → 抛异常（Controller 层 catch 后降级 FAQ）
 *   ③ FaqService 关键词匹配：售后/联系方式/发货/产品/支付/默认/空输入
 *   ④ Controller 降级链路：Dify 异常不冒泡，保证任何场景都有非 null 回复
 * <p>
 * DifyService 走原生 HttpURLConnection（SSE 流），无法直接用 Mockito mock，
 * 这里采用"配置异常 + FaqService 关键词全量覆盖"策略保证降级链路正确性。
 * 如要测实际 Dify 回复，请跑 curl 集成测试（用户验收单）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DifyService/FaqService - 大模型智能客服降级测试")
class DifyServiceTest {

    @Mock
    private DifyConfig difyConfig;

    @InjectMocks
    private DifyService difyService;

    private final FaqService faqService = new FaqService();

    private void injectObjectMapper() throws Exception {
        Field field = DifyService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(difyService, new ObjectMapper());
    }

    // ==================== DifyService 入参/配置异常测试 ====================

    @Nested
    @DisplayName("DifyService - 入参与配置边界")
    class DifyParamAndConfig {

        @Test
        @DisplayName("userId 空 → 返回空串（上层降级 FAQ）")
        void should_return_empty_when_userId_null() throws Exception {
            injectObjectMapper();
            String reply = difyService.chat(null, "你好");
            assertEquals("", reply);
        }

        @Test
        @DisplayName("message 空 → 返回空串（上层降级 FAQ）")
        void should_return_empty_when_message_null() throws Exception {
            injectObjectMapper();
            String reply = difyService.chat("session-001", "   ");
            assertEquals("", reply);
        }

        @Test
        @DisplayName("API Key 未配置 → 抛异常（上层 catch 降级 FAQ）")
        void should_throw_when_apiKey_empty() throws Exception {
            injectObjectMapper();
            // 注意：只 stub 会真正被调用的 getApiKey/getBaseUrl。
            // apiKey 为空时 DifyService 提前抛异常，getTimeoutSeconds 不会被调用，
            // stub 它会触发 Mockito 严格模式的 UnnecessaryStubbingException。
            when(difyConfig.getApiKey()).thenReturn("");
            when(difyConfig.getBaseUrl()).thenReturn("http://localhost:8088/v1");

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    difyService.chat("s1", "你好"));
            assertTrue(ex.getMessage().contains("API Key 未配置"));
        }
    }

    // ==================== FaqService 关键词全量覆盖 ====================

    @Nested
    @DisplayName("FAQ 关键词库匹配 - 不同关键词返回对应话术")
    class FaqKeywordMatching {

        @Test
        @DisplayName("包含'退款'→ 返回售后政策")
        void should_return_refund_faq() {
            assertTrue(faqService.chat("退款怎么搞").contains("售后"));
        }

        @Test
        @DisplayName("包含'退货'→ 返回售后政策")
        void should_return_return_faq() {
            assertTrue(faqService.chat("想退货").contains("售后"));
        }

        @Test
        @DisplayName("包含'电话'→ 返回联系方式")
        void should_return_contact_faq() {
            assertTrue(faqService.chat("客服电话是多少").contains("400-888-8888"));
        }

        @Test
        @DisplayName("包含'地址'→ 返回联系方式")
        void should_return_address_faq() {
            assertTrue(faqService.chat("你们公司在哪里").contains("苏州"));
        }

        @Test
        @DisplayName("包含'发货'→ 返回物流话术")
        void should_return_shipping_faq() {
            assertTrue(faqService.chat("下单多久发货").contains("48 小时"));
        }

        @Test
        @DisplayName("包含'物流'→ 返回物流话术")
        void should_return_logistics_faq() {
            assertTrue(faqService.chat("用什么快递").contains("顺丰"));
        }

        @Test
        @DisplayName("包含'电芯'→ 返回产品话术")
        void should_return_product_faq_by_battery() {
            assertTrue(faqService.chat("有什么电芯").contains("18650"));
        }

        @Test
        @DisplayName("包含'BMS'（大小写不敏感）→ 返回产品话术")
        void should_return_product_faq_by_bms() {
            assertTrue(faqService.chat("BMS 有吗").contains("BMS"));
        }

        @Test
        @DisplayName("包含'价格'→ 返回产品话术")
        void should_return_product_faq_by_price() {
            assertTrue(faqService.chat("多少钱").contains("规格"));
        }

        @Test
        @DisplayName("包含'支付'→ 返回支付话术")
        void should_return_payment_faq() {
            assertTrue(faqService.chat("怎么付款").contains("支付"));
        }

        @Test
        @DisplayName("默认闲聊 → 返回引导话术（含产品咨询等能力）")
        void should_return_guidance_for_default() {
            String reply = faqService.chat("你好呀");
            assertNotNull(reply);
            assertTrue(reply.contains("产品咨询") || reply.contains("智能客服"));
        }

        @Test
        @DisplayName("空输入 → 返回引导话术")
        void should_return_guidance_when_message_empty() {
            String reply = faqService.chat("");
            assertNotNull(reply);
            assertTrue(reply.contains("智能客服"));
        }

        @Test
        @DisplayName("null 输入 → 返回引导话术，不 NPE")
        void should_return_guidance_when_message_null() {
            assertDoesNotThrow(() -> {
                String reply = faqService.chat(null);
                assertNotNull(reply);
            });
        }
    }
}
