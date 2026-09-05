package com.greenchain.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车间响应VO（含3D数据）
 */
@Data
public class WorkshopVO {

    private Long id;

    private String name;

    private String location;

    private BigDecimal area;

    private String description;

    private String modelUrl;

    private LocalDateTime createTime;

    /** 车间内的工序节点列表 */
    private List<ProcessNodeVO> processes;
}