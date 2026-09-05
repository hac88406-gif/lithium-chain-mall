package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.ProcessNodeVO;
import com.greenchain.dto.response.WorkshopVO;
import com.greenchain.entity.Workshop;
import com.greenchain.mapper.WorkshopMapper;
import com.greenchain.repository.ProcessNodeRepository;
import com.greenchain.service.WorkshopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车间服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkshopServiceImpl implements WorkshopService {

    private final WorkshopMapper workshopMapper;
    private final ProcessNodeRepository processNodeRepository;

    @Override
    public PageResult<WorkshopVO> listAll(Long current, Long size) {
        Page<Workshop> page = new Page<>(current, size);
        IPage<Workshop> workshopPage = workshopMapper.selectPage(page, null);
        IPage<WorkshopVO> voPage = workshopPage.convert(this::convertToVO);
        return PageResult.of(voPage);
    }

    @Override
    public WorkshopVO getById(Long id) {
        Workshop workshop = workshopMapper.selectById(id);
        if (workshop == null) {
            throw new BusinessException(404, "车间不存在");
        }
        return convertToVO(workshop);
    }

    @Override
    public WorkshopVO add(Workshop workshop) {
        workshopMapper.insert(workshop);
        log.info("新增车间：{}", workshop.getName());
        return convertToVO(workshop);
    }

    @Override
    public WorkshopVO update(Workshop workshop) {
        if (workshop.getId() == null) {
            throw new BusinessException(400, "车间ID不能为空");
        }
        Workshop existing = workshopMapper.selectById(workshop.getId());
        if (existing == null) {
            throw new BusinessException(404, "车间不存在");
        }
        workshopMapper.updateById(workshop);
        log.info("编辑车间：{}", workshop.getName());
        return convertToVO(workshop);
    }

    @Override
    public void delete(Long id) {
        Workshop workshop = workshopMapper.selectById(id);
        if (workshop == null) {
            throw new BusinessException(404, "车间不存在");
        }
        workshopMapper.deleteById(id);
        log.info("删除车间：{}", workshop.getName());
    }

    private WorkshopVO convertToVO(Workshop workshop) {
        WorkshopVO vo = new WorkshopVO();
        vo.setId(workshop.getId());
        vo.setName(workshop.getName());
        vo.setLocation(workshop.getLocation());
        vo.setArea(workshop.getArea());
        vo.setDescription(workshop.getDescription());
        vo.setModelUrl(workshop.getModelUrl());
        vo.setCreateTime(workshop.getCreateTime());

        // 查询该车间的工序节点（从Neo4j）
        List<com.greenchain.entity.ProcessNode> nodes = processNodeRepository.findByWorkshop(workshop.getName());
        List<ProcessNodeVO> processVOs = new ArrayList<>();
        for (com.greenchain.entity.ProcessNode node : nodes) {
            processVOs.add(convertNodeToVO(node));
        }
        vo.setProcesses(processVOs);

        return vo;
    }

    private ProcessNodeVO convertNodeToVO(com.greenchain.entity.ProcessNode node) {
        ProcessNodeVO vo = new ProcessNodeVO();
        vo.setId(node.getId());
        vo.setName(node.getName());
        vo.setType(node.getType());
        vo.setDescription(node.getDescription());
        vo.setDuration(node.getDuration());

        if (node.getWorkshop() != null) {
            try {
                Workshop workshopEntity = workshopMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Workshop>()
                        .eq("name", node.getWorkshop())
                );
                vo.setWorkshop(workshopEntity);
            } catch (Exception e) {
                log.warn("查询车间信息失败: {}", node.getWorkshop(), e);
            }
        }

        if (node.getNextSteps() != null) {
            List<String> nextIds = node.getNextSteps().stream()
                    .map(com.greenchain.entity.ProcessNode::getId)
                    .collect(Collectors.toList());
            vo.setNextStepIds(nextIds);
        }
        return vo;
    }
}