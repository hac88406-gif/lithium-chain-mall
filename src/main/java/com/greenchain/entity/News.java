package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资讯/新闻实体
 * 对应 schema.sql 中 news 表：前台展示行业动态/公司新闻/技术资讯，
 * 管理后台通过 AdminNewsController 做 CRUD 维护。
 */
@Data
@TableName("news")
public class News {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资讯标题 */
    private String title;

    /** 分类：行业动态/公司新闻/技术资讯（与 schema.sql 种子枚举一致） */
    private String category;

    /** 摘要：列表页展示的简短描述 */
    private String summary;

    /** 正文：详情页富文本 HTML */
    @TableField("content")
    private String content;

    /** 封面图URL（列表/首页展示） */
    private String coverImage;

    /** 作者/来源 */
    private String author;

    /** 状态：0草稿 1发布 */
    private Integer status;

    /** 浏览量（详情页打开时 +1） */
    private Integer viewCount;

    /** 排序值，越大越靠前 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
