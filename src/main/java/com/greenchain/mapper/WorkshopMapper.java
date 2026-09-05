package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.Workshop;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车间Mapper接口
 */
@Mapper
public interface WorkshopMapper extends BaseMapper<Workshop> {
}