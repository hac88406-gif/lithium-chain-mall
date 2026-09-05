package com.greenchain.dto.response;

import com.greenchain.entity.Workshop;
import lombok.Data;
import java.util.List;

/**
 * 工序节点响应VO
 */
@Data
public class ProcessNodeVO {

    private String id;

    private String name;

    private String type;

    private String description;

    private Workshop workshop;

    private Integer duration;

    private List<String> nextStepIds;
}