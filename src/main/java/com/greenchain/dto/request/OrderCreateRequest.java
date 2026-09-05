package com.greenchain.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 创建订单请求DTO
 */
@Data
public class OrderCreateRequest {

    /** 购物车项ID列表（从购物车选中的项） */
    private List<Long> cartIds;

    /** 订单备注 */
    private String remark;
}