package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.request.NegotiationSubmitRequest;
import com.greenchain.dto.response.NegotiationVO;
import com.greenchain.dto.response.PageResult;
import com.greenchain.entity.Negotiation;
import com.greenchain.entity.Order;
import com.greenchain.entity.User;
import com.greenchain.mapper.NegotiationMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.mapper.UserMapper;
import com.greenchain.service.NegotiationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 商务洽谈服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NegotiationServiceImpl implements NegotiationService {

    private final NegotiationMapper negotiationMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegotiationVO submit(Long userId, NegotiationSubmitRequest request) {
        // 校验订单
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 创建洽谈
        Negotiation negotiation = new Negotiation();
        negotiation.setOrderId(request.getOrderId());
        negotiation.setUserId(userId);
        negotiation.setTitle(request.getTitle());
        negotiation.setContent(request.getContent());
        negotiation.setStatus("IN_PROGRESS");

        negotiationMapper.insert(negotiation);
        log.info("提交洽谈：用户{}，订单{}", userId, order.getOrderNo());
        return convertToVO(negotiation);
    }

    @Override
    public PageResult<NegotiationVO> listAll(Long current, Long size, String status) {
        Page<Negotiation> page = new Page<>(current, size);
        LambdaQueryWrapper<Negotiation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Negotiation::getStatus, status);
        }
        wrapper.orderByDesc(Negotiation::getCreateTime);
        IPage<Negotiation> negotiationPage = negotiationMapper.selectPage(page, wrapper);
        IPage<NegotiationVO> voPage = negotiationPage.convert(this::convertToVO);
        return PageResult.of(voPage);
    }

    @Override
    public NegotiationVO getByOrderId(Long orderId) {
        LambdaQueryWrapper<Negotiation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Negotiation::getOrderId, orderId);
        Negotiation negotiation = negotiationMapper.selectOne(wrapper);
        if (negotiation == null) {
            throw new BusinessException(404, "洽谈记录不存在");
        }
        return convertToVO(negotiation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegotiationVO reply(Long id, String reply) {
        Negotiation negotiation = negotiationMapper.selectById(id);
        if (negotiation == null) {
            throw new BusinessException(404, "洽谈记录不存在");
        }
        negotiation.setReply(reply);
        negotiation.setStatus("COMPLETED");
        negotiationMapper.updateById(negotiation);
        log.info("管理员回复洽谈：{}", negotiation.getTitle());
        return convertToVO(negotiation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegotiationVO updateStatus(Long id, String status) {
        Negotiation negotiation = negotiationMapper.selectById(id);
        if (negotiation == null) {
            throw new BusinessException(404, "洽谈记录不存在");
        }
        negotiation.setStatus(status);
        negotiationMapper.updateById(negotiation);
        return convertToVO(negotiation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Negotiation negotiation = negotiationMapper.selectById(id);
        if (negotiation == null) {
            throw new BusinessException(404, "洽谈记录不存在");
        }
        negotiationMapper.deleteById(id);
        log.info("删除洽谈记录：{}", id);
    }

    private NegotiationVO convertToVO(Negotiation negotiation) {
        NegotiationVO vo = new NegotiationVO();
        vo.setId(negotiation.getId());
        vo.setOrderId(negotiation.getOrderId());
        vo.setUserId(negotiation.getUserId());
        vo.setTitle(negotiation.getTitle());
        vo.setContent(negotiation.getContent());
        vo.setStatus(negotiation.getStatus());
        vo.setStatusName(getStatusName(negotiation.getStatus()));
        vo.setReply(negotiation.getReply());
        vo.setCreateTime(negotiation.getCreateTime());
        vo.setUpdateTime(negotiation.getUpdateTime());

        // 查询订单信息
        Order order = orderMapper.selectById(negotiation.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }

        // 查询用户信息
        User user = userMapper.selectById(negotiation.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }

        return vo;
    }

    private String getStatusName(String status) {
        if ("IN_PROGRESS".equals(status)) return "进行中";
        if ("COMPLETED".equals(status)) return "已完成";
        if ("REJECTED".equals(status)) return "已拒绝";
        return status;
    }
}