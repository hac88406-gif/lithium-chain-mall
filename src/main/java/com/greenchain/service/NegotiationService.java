package com.greenchain.service;

import com.greenchain.dto.request.NegotiationSubmitRequest;
import com.greenchain.dto.response.NegotiationVO;
import com.greenchain.dto.response.PageResult;

/**
 * 商务洽谈服务接口
 */
public interface NegotiationService {

    /**
     * 提交洽谈（客户端）
     */
    NegotiationVO submit(Long userId, NegotiationSubmitRequest request);

    /**
     * 查询洽谈列表（管理员）
     */
    PageResult<NegotiationVO> listAll(Long current, Long size, String status);

    /**
     * 根据订单ID查询洽谈
     */
    NegotiationVO getByOrderId(Long orderId);

    /**
     * 管理员回复洽谈
     */
    NegotiationVO reply(Long id, String reply);

    /**
     * 更新洽谈状态
     */
    NegotiationVO updateStatus(Long id, String status);

    /**
     * 删除洽谈
     */
    void delete(Long id);
}