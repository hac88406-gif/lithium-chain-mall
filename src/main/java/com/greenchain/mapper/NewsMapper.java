package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资讯 Mapper：管理后台 CRUD + 前台列表详情共用
 * 继承 BaseMapper<News> 即可获得 insert/selectById/selectList/update/deleteById，
 * 分页查用 Wrapper + Page，无需自定义 XML SQL。
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
}
