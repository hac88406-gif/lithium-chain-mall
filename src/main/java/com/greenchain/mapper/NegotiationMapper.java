package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.Negotiation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商务洽谈Mapper接口
 */
@Mapper
public interface NegotiationMapper extends BaseMapper<Negotiation> {
}