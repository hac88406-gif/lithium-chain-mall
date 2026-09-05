-- =============================================================
-- 绿链锂电采购商城 · 数据库初始化脚本（完整单文件版）
-- 设计容量：买家端 + 管理后台（管理员/角色/权限）+ 车间/工艺/议价/
--          商务合作/人工客服消息/评价/操作审计/购物车/支付流水 共 20 张表
-- 约定：
--   ① 所有表均使用 CREATE TABLE IF NOT EXISTS，遇错不中断后续执行；
--   ② DROP 顺序严格按"子表→父表"依赖反向排列，避免外键约束报错；
--   ③ 保留关键字（`order`/`user`）全部用反引号包裹；
--   ④ 枚举/CHECK 用 MySQL TINYINT + 注释说明，应用层（DTO @Valid）兜底。
-- =============================================================

CREATE DATABASE IF NOT EXISTS green_chain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE green_chain;

-- ========= ⚠️ 安全警告：以下 DROP 语句仅用于首次初始化空数据库！ =========
-- 如果 green_chain 库中已有业务数据，请务必注释或删除以下 DROP 段，
-- 直接执行后续的 CREATE TABLE IF NOT EXISTS 建表语句即可。
-- 线上环境禁止执行此脚本，应使用增量迁移脚本。
-- ====================================================================
-- DROP：按依赖反向（先子后父）
DROP TABLE IF EXISTS service_request_message;
DROP TABLE IF EXISTS service_request_review;
DROP TABLE IF EXISTS customer_service_request;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS business_cooperation;
DROP TABLE IF EXISTS t_negotiation;
DROP TABLE IF EXISTS payment_transaction;
DROP TABLE IF EXISTS after_sale;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS t_cart;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_admin;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS news;
DROP TABLE IF EXISTS t_workshop;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS carousel;
DROP TABLE IF EXISTS `user`;

-- ========= 1. 基础：用户 / 品类 / 商品 / 轮播 =========
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (`username`),
    INDEX idx_phone (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买家用户表';

CREATE TABLE IF NOT EXISTS category (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS product (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT COMMENT '分类ID',
    `description` TEXT COMMENT '商品描述',
    `image` VARCHAR(255) COMMENT '商品图片URL',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `sales` INT DEFAULT 0 COMMENT '销量（支付成功后原子累加）',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0下架 1上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_id (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS carousel (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) COMMENT '轮播标题',
    `image` VARCHAR(255) NOT NULL COMMENT '轮播图片URL',
    `link` VARCHAR(255) COMMENT '跳转链接',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图表';

CREATE TABLE IF NOT EXISTS news (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL COMMENT '资讯标题',
    `category` VARCHAR(50) DEFAULT '行业动态' COMMENT '分类(行业动态/公司新闻/技术资讯)',
    `summary` VARCHAR(500) COMMENT '摘要',
    `content` TEXT COMMENT '正文内容',
    `cover_image` VARCHAR(255) COMMENT '封面图URL',
    `author` VARCHAR(50) COMMENT '作者',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0草稿 1发布',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (`category`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯/新闻表';

-- ========= 2. 交易域：地址 / 订单 / 订单项 / 售后 / 支付流水 =========
CREATE TABLE IF NOT EXISTS user_address (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
    `province` VARCHAR(50) COMMENT '省份',
    `city` VARCHAR(50) COMMENT '城市',
    `district` VARCHAR(50) COMMENT '区县',
    `detail` VARCHAR(500) COMMENT '详细地址',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认 0否 1是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

CREATE TABLE IF NOT EXISTS cart_item (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `name` VARCHAR(200) COMMENT '商品名称快照',
    `image` VARCHAR(255) COMMENT '商品图片快照',
    `price` DECIMAL(10,2) NOT NULL COMMENT '加入购物车时的价格快照',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `stock` INT DEFAULT 0 COMMENT '库存(冗余展示用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_product_id (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车项表(扁平项结构，兼容老版前端)';

CREATE TABLE IF NOT EXISTS t_cart (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车主表ID(新版Service层用)',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    INDEX idx_user_id (`user_id`),
    INDEX idx_product_id (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车主表(MyBatis-Plus逻辑删除支持)';

CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `address_id` BIGINT COMMENT '地址ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `total_quantity` INT NOT NULL COMMENT '总商品数量',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending待付款 paid已付款 shipped已发货 completed已完成 cancelled已取消',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_order_no (`order_no`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

CREATE TABLE IF NOT EXISTS order_item (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `name` VARCHAR(200) COMMENT '商品名称快照',
    `image` VARCHAR(255) COMMENT '商品图片快照',
    `spec` VARCHAR(100) COMMENT '规格',
    `price` DECIMAL(10,2) NOT NULL COMMENT '下单时单价快照',
    `quantity` INT NOT NULL COMMENT '数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE IF NOT EXISTS after_sale (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '售后单ID',
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `after_sale_type` TINYINT NOT NULL COMMENT '售后类型 1退款 2退货(仅允许此二值,非法值DTO层拦截)',
    `reason` VARCHAR(512) COMMENT '申请理由',
    `evidence` VARCHAR(2048) COMMENT '凭证图片URL,多张逗号分隔',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0待审核 1同意 2拒绝',
    `reject_reason` VARCHAR(512) COMMENT '拒绝理由(拒绝时必填)',
    `refund_amount` DECIMAL(10,2) COMMENT '实际退款金额',
    `review_admin_id` BIGINT COMMENT '审核管理员ID',
    `review_time` DATETIME COMMENT '审核时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (`order_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后申请表';

CREATE TABLE IF NOT EXISTS payment_transaction (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付流水ID',
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号(冗余,方便回调查询)',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `payment_no` VARCHAR(64) NOT NULL COMMENT '平台支付流水号(全局唯一)',
    `channel` VARCHAR(20) DEFAULT 'mock' COMMENT '支付通道 mock/wechat/alipay',
    `transaction_id` VARCHAR(64) COMMENT '第三方交易号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
    `type` VARCHAR(20) NOT NULL COMMENT '事务类型 payment支付 refund退款',
    `ref_payment_no` VARCHAR(64) COMMENT '关联原支付流水号(退款单使用)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'created' COMMENT 'created预下单 success成功 fail失败 refunding退款中 refunded已退款 closed已关闭',
    `sign_params` TEXT COMMENT '预下单签名参数(json)',
    `sign` VARCHAR(128) COMMENT '回调/发起签名值',
    `pay_time` DATETIME COMMENT '支付成功时间',
    `refund_time` DATETIME COMMENT '退款完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_no (`payment_no`),
    UNIQUE KEY uk_transaction_id (`transaction_id`),
    INDEX idx_order_id (`order_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表(状态机+幂等)';

-- ========= 3. RBAC：角色 / 权限 / 管理员账号 =========
CREATE TABLE IF NOT EXISTS sys_role (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    UNIQUE KEY uk_role_name (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
    `name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_key` VARCHAR(100) NOT NULL COMMENT '权限标识(如sys:product:list)',
    `type` TINYINT NOT NULL COMMENT '类型 1菜单 2按钮',
    `sort` INT DEFAULT 0 COMMENT '排序',
    UNIQUE KEY uk_permission_key (`permission_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY idx_role_id (`role_id`),
    KEY idx_permission_id (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS sys_admin (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    `username` VARCHAR(100) NOT NULL COMMENT '登录用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role_id` BIGINT DEFAULT NULL COMMENT '绑定角色ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (`username`),
    KEY idx_role_id (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统管理员表';

-- ========= 4. 工厂域：车间 =========
CREATE TABLE IF NOT EXISTS t_workshop (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '车间ID',
    `name` VARCHAR(100) NOT NULL COMMENT '车间名称',
    `location` VARCHAR(200) COMMENT '车间位置',
    `area` DECIMAL(10,2) COMMENT '车间面积(平方米)',
    `description` TEXT COMMENT '车间描述',
    `model_url` VARCHAR(500) COMMENT '3D模型文件URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    UNIQUE KEY uk_workshop_name (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车间表';

-- ========= 5. 业务洽谈：议价 / 商务合作 =========
CREATE TABLE IF NOT EXISTS t_negotiation (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '议价单ID',
    `order_id` BIGINT COMMENT '关联订单ID(可为空,仅提交后绑定)',
    `user_id` BIGINT NOT NULL COMMENT '提交用户ID',
    `product_id` BIGINT COMMENT '意向商品ID',
    `title` VARCHAR(200) COMMENT '洽谈主题',
    `expect_price` DECIMAL(10,2) COMMENT '期望单价',
    `quantity` INT COMMENT '意向数量',
    `content` TEXT COMMENT '洽谈内容/详细说明',
    `attachments` VARCHAR(2048) COMMENT '附件URL,多文件逗号分隔',
    `status` VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS进行中 COMPLETED已完成 REJECTED已拒绝',
    `reply` TEXT COMMENT '管理员回复',
    `reply_admin_id` BIGINT COMMENT '处理管理员ID',
    `reply_time` DATETIME COMMENT '回复时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='议价/商务洽谈表';

CREATE TABLE IF NOT EXISTS business_cooperation (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商务合作申请ID',
    `user_id` BIGINT COMMENT '提交用户ID(登录用户关联,未登录为空)',
    `type` VARCHAR(20) DEFAULT 'purchase' COMMENT '合作类型 purchase采购 intention商务 media媒体 other其他',
    `company_name` VARCHAR(200) COMMENT '公司名称',
    `company_address` VARCHAR(500) COMMENT '公司地址',
    `contact_person` VARCHAR(50) NOT NULL COMMENT '联系人',
    `position` VARCHAR(50) COMMENT '职位',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `email` VARCHAR(100) COMMENT '邮箱',
    `requirement_type` VARCHAR(100) COMMENT '需求类型',
    `budget` VARCHAR(50) COMMENT '预算范围',
    `intention` VARCHAR(500) COMMENT '合作意向描述',
    `delivery_cycle` VARCHAR(100) COMMENT '期望交付周期',
    `media_title` VARCHAR(200) COMMENT '媒体合作-报道标题',
    `media_format` VARCHAR(50) COMMENT '媒体合作-报道形式',
    `media_date` VARCHAR(20) COMMENT '媒体合作-期望发布日期',
    `attachments` VARCHAR(2048) COMMENT '附件URL(多文件逗号分隔)',
    `remark` VARCHAR(500) COMMENT '备注',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0待处理 1处理中 2已回复 3已归档',
    `reply` TEXT COMMENT '官方回复',
    `reply_by` VARCHAR(100) COMMENT '回复管理员用户名',
    `reply_time` DATETIME COMMENT '回复时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`),
    INDEX idx_type (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商务合作申请表';

-- ========= 6. 客服：工单 / 消息 / 评价 =========
CREATE TABLE IF NOT EXISTS customer_service_request (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '人工客服工单ID',
    `user_id` BIGINT COMMENT '提交用户ID(未登录可为空)',
    `name` VARCHAR(50) COMMENT '联系人姓名',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `email` VARCHAR(100) COMMENT '邮箱',
    `source` VARCHAR(20) DEFAULT 'manual' COMMENT '来源 manual手动提交 ai AI客服转人工',
    `question` VARCHAR(500) COMMENT '问题类型',
    `content` TEXT COMMENT '初始问题描述',
    `status` TINYINT DEFAULT 0 COMMENT '0待应答 1活跃中 2已关闭(超时/人工) 3已完成(已评价)',
    `reply` TEXT COMMENT '首次官方回复(兼容老字段)',
    `reply_by` VARCHAR(100) COMMENT '回复管理员用户名',
    `reply_time` DATETIME COMMENT '回复时间',
    `chat_session_id` VARCHAR(64) COMMENT 'AI智能客服对话sessionId(转人工时携带)',
    `chat_history` TEXT COMMENT 'AI对话历史快照(json)',
    `admin_unread_count` INT DEFAULT 0 COMMENT '管理员未读消息数(展示红点)',
    `user_unread_count` INT DEFAULT 0 COMMENT '客户未读消息数',
    `last_msg_time` DATETIME COMMENT '最后消息时间(排序用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`),
    INDEX idx_last_msg_time (`last_msg_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工客服工单表';

CREATE TABLE IF NOT EXISTS service_request_message (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '聊天消息ID',
    `request_id` BIGINT NOT NULL COMMENT '关联工单ID(customer_service_request.id)',
    `sender_type` VARCHAR(20) NOT NULL COMMENT '发送方 customer客户 admin管理员',
    `sender_id` BIGINT COMMENT '发送者ID(管理员对应sys_admin.id,客户对应user.id)',
    `sender_name` VARCHAR(50) COMMENT '发送者展示名(冗余)',
    `content` TEXT NOT NULL COMMENT '消息文本内容',
    `msg_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型 text文本 image图片 system系统',
    `attachments` VARCHAR(2048) COMMENT '附件URL(图片/文档,多文件逗号分隔)',
    `is_read` TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_request_id (`request_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服聊天消息表';

CREATE TABLE IF NOT EXISTS service_request_review (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    `request_id` BIGINT NOT NULL COMMENT '关联工单ID',
    `rating` TINYINT NOT NULL COMMENT '星级 1-5',
    `tags` VARCHAR(500) COMMENT '评价标签(逗号分隔,如 响应快,专业,耐心)',
    `content` VARCHAR(500) COMMENT '客户简短评价文字',
    `suggestion` VARCHAR(500) COMMENT '改进建议',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (`request_id`) COMMENT '一个工单仅允许一次评价'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工客服评价表';

-- ========= 7. 管理后台操作审计日志 =========
CREATE TABLE IF NOT EXISTS sys_oper_log (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审计日志ID',
    `admin_id` BIGINT COMMENT '操作管理员ID(为空=未登录异常请求)',
    `username` VARCHAR(100) COMMENT '管理员用户名(冗余,避免账号删除后无法溯源)',
    `role_name` VARCHAR(100) COMMENT '管理员角色名(冗余)',
    `operation` VARCHAR(200) COMMENT '操作摘要(如 PUT /api/admin/product/1 更新商品)',
    `http_method` VARCHAR(10) COMMENT 'HTTP方法 GET/POST/PUT/DELETE',
    `request_uri` VARCHAR(500) COMMENT '请求路径(含查询串)',
    `request_params` TEXT COMMENT '请求参数JSON(GET为query串,POST/PUT为body)',
    `response_result` TEXT COMMENT '响应结果JSON(截断前2000字符,仅留code和message)',
    `ip` VARCHAR(50) COMMENT '客户端来源IP',
    `user_agent` VARCHAR(500) COMMENT '浏览器UA',
    `status` INT COMMENT '响应业务code(200=成功 4xx=业务失败 5xx=异常)',
    `cost_ms` BIGINT COMMENT '接口耗时(毫秒)',
    `error_msg` VARCHAR(1000) COMMENT '异常堆栈摘要(仅出错时有值)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_admin_id (`admin_id`),
    INDEX idx_username (`username`),
    INDEX idx_create_time (`create_time`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理后台操作审计日志';

-- =============================================================
-- 初始化种子数据（与 schema-permission.sql + demo_init.sql 保持一致）
-- =============================================================

-- 1) 权限树(固定ID以便后续引用)
INSERT IGNORE INTO sys_permission (`id`, `parent_id`, `name`, `permission_key`, `type`, `sort`) VALUES
(1,  0, '仪表盘',           'sys:dashboard',           1, 1),
(2,  0, '商品分类管理',     'sys:category:list',       1, 2),
(3,  2, '新增分类',         'sys:category:add',        2, 1),
(4,  2, '编辑分类',         'sys:category:edit',       2, 2),
(5,  2, '删除分类',         'sys:category:delete',     2, 3),
(6,  0, '商品管理',         'sys:product:list',        1, 3),
(7,  6, '新增商品',         'sys:product:add',         2, 1),
(8,  6, '编辑商品',         'sys:product:edit',        2, 2),
(9,  6, '删除商品',         'sys:product:delete',      2, 3),
(10, 6, '导出商品',         'sys:product:export',      2, 4),
(11, 0, '订单管理',         'sys:order:list',          1, 4),
(12,11, '编辑订单',         'sys:order:edit',          2, 1),
(13,11, '删除订单',         'sys:order:delete',        2, 2),
(14,11, '导出订单',         'sys:order:export',        2, 3),
(15, 0, '买家用户管理',     'sys:user:list',           1, 5),
(16,15, '编辑用户',         'sys:user:edit',           2, 1),
(17,15, '禁用用户',         'sys:user:delete',         2, 2),
(18, 0, '资讯管理',         'sys:news:list',           1, 6),
(19,18, '新增资讯',         'sys:news:add',            2, 1),
(20,18, '编辑资讯',         'sys:news:edit',           2, 2),
(21,18, '删除资讯',         'sys:news:delete',         2, 3),
(22, 0, '首页轮播管理',     'sys:carousel:list',       1, 7),
(23,22, '新增轮播',         'sys:carousel:add',        2, 1),
(24,22, '编辑轮播',         'sys:carousel:edit',       2, 2),
(25,22, '删除轮播',         'sys:carousel:delete',     2, 3),
(26, 0, '网站设置',         'sys:settings',            1, 8),
(27, 0, '系统权限',         'sys:permission',          1, 9),
(28,27, '角色管理',         'sys:role:list',           1, 1),
(29,28, '新增角色',         'sys:role:add',            2, 1),
(30,28, '编辑角色',         'sys:role:edit',           2, 2),
(31,28, '删除角色',         'sys:role:delete',         2, 3),
(32,27, '管理员账号管理',   'sys:admin:list',          1, 2),
(33,32, '新增管理员',       'sys:admin:add',           2, 1),
(34,32, '编辑管理员',       'sys:admin:edit',          2, 2),
(35,32, '删除管理员',       'sys:admin:delete',        2, 3),
(36, 0, '售后管理',         'sys:aftersale:list',      1, 10),
(37,36, '售后审核',         'sys:aftersale:review',    2, 1),
(38, 0, '图片上传',         'sys:upload:image',        2, 11),
(46, 0, '全屏数据大屏',     'sys:dashboard:screen',    1, 19),
(39, 0, '库存与仓储',       'wms:stock:list',          1, 12),
(40, 0, '财务对账',         'finance:reconcile:list',  1, 13),
(41, 0, '供应商管理',       'purchase:supplier:list',  1, 14),
(42, 0, '车间工艺管理',     'mfg:workshop:list',       1, 15),
(43, 0, '操作审计日志',     'sys:audit:list',          1, 16),
(44, 0, '商务合作申请',     'sys:cooperation:list',    1, 17),
(45, 0, '人工客服请求',     'sys:service:list',        1, 18);

-- 2) 超级管理员（密码 admin123，BCrypt 真实哈希，已用 BCryptPasswordEncoder 实测 matches() 通过）
INSERT IGNORE INTO sys_admin (`id`, `username`, `password`, `real_name`, `role_id`, `status`, `create_time`) VALUES
(1, 'admin', '$2a$10$oeV2Jc7catKAyKbP9nGFA.wfW1ItbX5RMAmbVfS1fNKEN3emJDwnG', '超级管理员', NULL, 1, NOW());

-- 2.1) 销售运营账号（密码 op123456，BCrypt 真实哈希；role_id=2 对应下方销售运营角色）
INSERT IGNORE INTO sys_admin (`id`, `username`, `password`, `real_name`, `role_id`, `status`, `create_time`) VALUES
(2, 'operator', '$2a$10$ScKrKF3LoIcvTSnYn4PAJeCrc4IfKwFQp0Dcei6FW0upIn9VnEeha', '销售运营', 2, 1, NOW());

-- 3) 销售运营角色(给普通运营账号使用,不含系统类权限)
INSERT IGNORE INTO sys_role (`id`, `role_name`, `remark`, `status`) VALUES
(1, '超级管理员',   '拥有全部权限(roleId=1在前端/后端都会被豁免)', 1),
(2, '销售运营',     '负责订单/售后/客户/商品/品类业务,不含系统配置与审计', 1);

-- 4) 车间种子(与 schema-permission.sql 一致)
INSERT IGNORE INTO t_workshop (`id`, `name`, `location`, `area`, `description`, `model_url`) VALUES
(1, '冲压车间', 'A栋1层', 2000.00, '极片冲压加工车间', '/models/stamping.glb'),
(2, '卷绕车间', 'A栋2层', 1500.00, '电芯卷绕车间',   '/models/winding.glb'),
(3, '化成车间', 'B栋1层', 3000.00, '电芯化成车间',   '/models/formation.glb'),
(4, '老化车间', 'B栋2层', 2500.00, '电池老化测试车间', '/models/aging.glb'),
(5, '封装车间', 'C栋1层', 1800.00, 'PACK封装车间',  '/models/packaging.glb'),
(6, '检测车间', 'C栋2层', 1200.00, '成品检测车间',   '/models/testing.glb');

-- 5) 分类种子(与原 schema.sql 一致, 加了 ID 方便后续引用)
INSERT IGNORE INTO category (`id`, `name`, `parent_id`, `sort`, `status`) VALUES
(1, '锂电池',     0, 1, 1),
(2, '动力电池',   0, 2, 1),
(3, '储能电池',   0, 3, 1),
(4, '电池管理系统', 0, 4, 1),
(5, '充电配件',   0, 5, 1);

-- 6) 商品种子(与原 schema.sql 一致, ID 显式写入)
INSERT IGNORE INTO product (`id`, `name`, `category_id`, `description`, `image`, `price`, `stock`, `sales`, `status`) VALUES
(1, '18650锂电池组 48V 【LC-128E1】A品电芯·电动车专用',     1, '高能量密度18650锂电池组，A品级电芯，适用于两轮电动车、光伏储能等场景，循环寿命≥2000次', '/images/18650.png',              1280.00, 1000, 520, 1),
(2, '21700锂电池组 60V 【LC-168E2】高倍率·电摩长续航',        1, '大容量21700动力型锂电池组，适配60V电摩及高速车，支持3C高倍率放电，续航持久',                  '/images/21700.png',              1680.00,  800, 380, 1),
(3, '48V动力电池包 【PB-258P1】CTP标准·液冷快换',             2, '新能源乘用车级48V动力包，CTP集成+液冷温控，支持快充(0.5C/30min)，IP68',                        '/images/48Velectrocar.png',      2580.00,  500, 210, 1),
(4, '家用储能电池 10kWh 【ES-880H1】堆叠式·光储一体机',       3, '家庭户用储能系统，10kWh堆叠式磷酸铁锂，兼容离网/并网，光伏自发自用，10年质保',                '/images/portable-power.jpg.png', 8800.00,  200,  85, 1),
(5, '便携式储能电源 2kWh 【ES-288P2】户外便携·多场景',        3, '户外便携储能电源2000Wh输出，支持太阳能补电/110V-220V双压，露营/医疗应急首选',                 '/images/portable-power.jpg.png', 2880.00,  300, 150, 1),
(6, 'BMS电池管理系统 【BM-098B1】主从架构·云监控',            4, '16S-24S主从架构BMS，带AFE高精度采集、过充过放保护、蓝牙/4G远程监控，车规级EMC',                 '/images/bms.png',                 980.00, 1000, 620, 1);

-- 7) 轮播种子
INSERT IGNORE INTO carousel (`id`, `title`, `image`, `link`, `sort`, `status`) VALUES
(1, '绿链锂电 · 绿色能源全链路解决方案', '/images/globe.jpg',              '/products', 1, 1),
(2, '动力电池/储能系统工厂直供 · 工厂资质保障', '/images/factory-building.png', '/products', 2, 1),
(3, '科技赋能 · A级电芯 循环寿命领先',         '/images/science.jpeg',         '/products', 3, 1);

-- 8) 资讯种子(用于 NewsManagement 管理页首次打开有数据)
INSERT IGNORE INTO news (`id`, `title`, `category`, `summary`, `content`, `author`, `status`, `view_count`, `sort`) VALUES
(1, '锂电池行业发展趋势分析 2026',    '行业动态', '全球动力与储能锂电需求持续走高,新型材料体系开始应用.',
    '根据最新行业报告,2026年全球动力锂电出货量预计同比增长35%,磷酸铁锂与三元材料在不同场景各自保持优势...', '编辑部', 1, 128, 1),
(2, '绿链锂电PACK工厂正式投产',     '公司新闻', '公司第三条PACK自动化产线量产,日产模组15000组,交付周期缩短40%.',
    '绿链锂电于今日在总部举行新产线投产仪式,市长、行业协会、合作伙伴等200人出席...',                    '绿链运营', 1,  96, 2),
(3, '固态电池技术最新突破解读',      '技术资讯', '半固态电解质能量密度突破420Wh/kg,预计2027年进入车载量产阶段.',
    '近日多家实验室公布半固态电池最新进展:循环寿命突破1500次(保持80%容量),制造成本相比全固态下降65%...', '技术组',   1, 204, 3),
(4, '新能源汽车销量再创月度新高',    '行业动态', '8月国内新能源乘用车销量首次突破120万辆,渗透率达48%.',
    '乘联会数据显示,2026年8月国内新能源车零售121.3万辆,同比增长29.6%...',                             '编辑部', 1,  78, 4),
(5, '储能政策频出,工商业储能迎来爆发期', '行业动态', '峰谷价差拉大 + 两部制电价改革,工商业储能IRR回到合理区间.',
    '近期多省发布最新分时电价政策,峰谷价差超过0.7元/千瓦时的省份增加至18个...',                        '政策组', 1,  56, 5);



CREATE TABLE IF NOT EXISTS payment_transaction (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '流水ID',
                                                   order_id BIGINT NOT NULL COMMENT '关联订单ID',
                                                   order_no VARCHAR(64) NOT NULL COMMENT '订单号',
                                                   user_id BIGINT NOT NULL COMMENT '用户ID',
                                                   payment_no VARCHAR(64) NOT NULL COMMENT '平台支付流水号(幂等主键)',
                                                   channel VARCHAR(32) COMMENT '支付通道标识',
                                                   transaction_id VARCHAR(64) COMMENT '第三方交易号',
                                                   amount DECIMAL(10,2) NOT NULL COMMENT '金额',
                                                   type VARCHAR(20) NOT NULL COMMENT '事务类型 payment支付 refund退款',
                                                   ref_payment_no VARCHAR(64) COMMENT '关联原支付流水号(退款单使用)',
                                                   status VARCHAR(20) NOT NULL DEFAULT 'created' COMMENT 'created预下单 success成功 fail失败 refunding退款中 refunded已退款 closed已关闭',
                                                   sign_params TEXT COMMENT '预下单签名参数(json)',
                                                   sign VARCHAR(128) COMMENT '回调/发起签名值',
                                                   pay_time DATETIME COMMENT '支付成功时间',
                                                   refund_time DATETIME COMMENT '退款完成时间',
                                                   create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                   update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                   UNIQUE KEY uk_payment_no (payment_no),
                                                   UNIQUE KEY uk_transaction_id (transaction_id),
                                                   INDEX idx_order_id (order_id),
                                                   INDEX idx_user_id (user_id),
                                                   INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表(状态机+幂等)';