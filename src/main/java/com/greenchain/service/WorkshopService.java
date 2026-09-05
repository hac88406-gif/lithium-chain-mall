package com.greenchain.service;

import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.WorkshopVO;
import com.greenchain.entity.Workshop;

/**
 * 车间服务接口
 */
public interface WorkshopService {

    /**
     * 查询车间列表
     */
    PageResult<WorkshopVO> listAll(Long current, Long size);

    /**
     * 查询车间详情（含3D数据和工序）
     */
    WorkshopVO getById(Long id);

    /**
     * 新增车间（管理员）
     */
    WorkshopVO add(Workshop workshop);

    /**
     * 编辑车间（管理员）
     */
    WorkshopVO update(Workshop workshop);

    /**
     * 删除车间（管理员）
     */
    void delete(Long id);
}