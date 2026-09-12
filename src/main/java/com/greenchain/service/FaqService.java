package com.greenchain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 本地 FAQ 关键词库兜底服务
 * <p>
 * 当所有 AI 大模型都不可用时（Dify 挂了 / Dify 内部云端本地都失败），
 * 根据用户输入中的关键词匹配返回预设话术，保证任何环境都能给出合理回复。
 * <p>
 * 匹配规则：用户消息转小写后，包含任一关键词即命中对应回复；多个关键词可同时命中，
 * 按优先级顺序返回（售后 > 联系方式 > 发货 > 产品 > 支付 > 默认）。
 */
@Slf4j
@Service
public class FaqService {

    public String chat(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "您好！我是绿链锂电智能客服，可以为您解答产品咨询、联系方式、售后政策等问题~";
        }
        String lower = message.toLowerCase();

        if (containsAny(lower, "退款", "退货", "售后", "退钱", "不要了")) {
            return "关于售后政策：您确认收货后可在【我的订单】页面点击「申请售后」，选择退款或退货并填写理由。一个订单仅可申请一次，我们会在 1-3 个工作日内审核处理~";
        }
        if (containsAny(lower, "电话", "联系方式", "地址", "联系", "怎么找你们", "客服电话", "哪里", "在哪")) {
            return "绿链锂电客服热线：400-888-8888（工作日 9:00-18:00）。公司地址：江苏省苏州市工业园区绿链产业园 A 栋。也可以通过右下角在线咨询或添加企业微信与我们联系~";
        }
        if (containsAny(lower, "发货", "物流", "快递", "几天到", "什么时候发", "配送")) {
            return "下单后我们会在 48 小时内安排发货（节假日顺延），默认顺丰速运。发货后您可以在【我的订单】中查看物流状态。如需加急请在下单备注中说明~";
        }
        if (containsAny(lower, "18650", "21700", "电芯", "bms", "电池", "产品", "规格", "价格", "型号", "容量", "电压", "多少钱", "怎么卖")) {
            return "绿链锂电主营：18650/21700 动力电芯、BMS 电池管理系统、12V/24V/48V 电池模组、便携储能电源等全系列绿色能源产品。您可以在首页或商品页浏览详细规格和价格，如需批量采购欢迎提交【合作申请】，我们会有专属顾问为您报价~";
        }
        if (containsAny(lower, "支付", "付款", "怎么付", "支持什么支付")) {
            return "目前平台支持模拟支付流程：下单后点击「立即支付」即可完成支付状态流转。正式上线后将接入微信支付、支付宝对公转账等多种方式，敬请期待~";
        }
        return "您好！我是绿链锂电智能客服，可以为您解答：① 产品咨询（18650/21700 电芯、BMS、电池模组等）；② 联系方式；③ 售后政策；④ 发货物流等问题。您也可以拨打客服电话 400-888-8888 或添加企业微信~";
    }

    private boolean containsAny(String lowerMsg, String... keywords) {
        if (lowerMsg == null || keywords == null) return false;
        for (String kw : keywords) {
            if (kw != null && lowerMsg.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
