<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.15-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vue.js&logoColor=white" alt="Vue">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-6.x-DC382D?logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Neo4j-4%2F5-4581C3?logo=neo4j&logoColor=white" alt="Neo4j">
  <img src="https://img.shields.io/badge/MinIO-Object_Storage-C72E49?logo=minio&logoColor=white" alt="MinIO">
  <img src="https://img.shields.io/badge/Dify-AI%20Agent-10B981?logoColor=white" alt="Dify">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

<h1 align="center">🔋 绿链锂电 · 全链路采购商城与数字化智能制造平台</h1>

<p align="center">
  <b>面向锂电新能源行业的 B2B2C 一体化平台：买家端商城 + 销售运营后台 + 超级管理员后台 + 数字孪生工艺图谱大屏</b>
</p>

<p align="center">
  <a href="#-项目亮点">✨ 项目亮点</a> ·
  <a href="#-功能模块">📦 功能模块</a> ·
  <a href="#-技术栈">🛠 技术栈</a> ·
  <a href="#-快速启动">🚀 快速启动</a> ·
  <a href="#-核心亮点详解">💡 核心亮点</a> ·
  <a href="#-演示账号">🔑 演示账号</a>
</p>

---

## 🎯 项目简介

**绿链锂电采购平台**是一个围绕锂电新能源产业链打造的全链路数字化系统，既包含面向终端客户的 **B2C 电商商城**（商品浏览、下单、支付、售后、智能客服），也包含面向内部运营的 **B2B 管理后台**（订单履约、商品库存、售后审核、商务合作、RBAC 权限与操作审计），同时基于 **Neo4j 图数据库 + ECharts/Three.js** 构建了**数字孪生工艺图谱**，可视化展示锂电从「配料 → 涂布 → 辊压 → 分切 → 叠片/卷绕 → 装配 → 化成分容 → PACK 模组」的完整工艺流程，并支持**产品全生命周期追溯**与**风险工序定位分析**。

> 本项目为个人毕业设计/作品集项目，**所有技术选型与实现细节均面向真实生产场景进行设计**：支付幂等与回调防伪造、订单并发安全、防重放攻击、操作审计日志、AI 智能客服降级兜底等均可直接迁移至生产环境。

---

## ✨ 项目亮点

| 亮点 | 一句话说明 |
|------|-----------|
| 🔌 **支付体系策略模式** | Mock 支付网关 + 策略接口可扩展微信/支付宝，幂等流水表 + 签名验签 + 订单状态机 |
| 🔗 **订单一致性保障** | 防重令牌切面 + 改价免疫（DB 价格重算）+ 条件 UPDATE 原子关单/库存回补 |
| 🧬 **数字孪生工艺图谱** | Neo4j 工序节点 + NEXT_STEP 关系边，ECharts Graph 高亮联动，Three.js 工厂 3D 展示 |
| 🛡 **RBAC + 三拦截器链** | 鉴权 → 细粒度权限 → 操作审计，三拦截器有序串联，ThreadLocal 权限上下文 |
| 🤖 **AI 客服双轨降级** | Dify SSE 流式响应（deepseek 云端 + Ollama 本地），异常自动降级 FAQ 关键词匹配 |
| 📊 **全屏数据驾驶舱** | 1920×1080 设计稿 transform 缩放适配，6 大核心指标 + 4 张图表实时刷新 |
| 🚀 **Docker 一键编排** | MySQL/Redis/Neo4j/MinIO/Backend/Frontend 6 服务 healthcheck 顺序启动 |

---

## 📦 功能模块

### 🛒 买家端（17 个页面）
| 模块 | 页面 |
|------|------|
| 首页与内容 | 首页、关于我们、联系我们、商务合作、合作介绍、新闻资讯、下载页 |
| 商品中心 | 商品列表、商品详情（SKU 库存校验、缺货提示、实时库存上限）、工艺图谱（数字孪生） |
| 交易流程 | 购物车（游客+登录双模式）、结算下单、订单中心（5 种状态生命周期按钮） |
| 售后客服 | 售后申请（9 图凭证上传）、智能客服（Dify SSE 流 + FAQ 降级）、工单系统 |
| 个人中心 | 我的足迹、我的应用、个人信息管理 |

### 📈 销售运营后台（15 个页面 + 1 数据大屏）
| 模块 | 页面 |
|------|------|
| 工作台 | 数据仪表盘 Dashboard、全屏数据驾驶舱 DataScreen |
| 商品中心 | 商品管理、分类管理、轮播图管理、库存管理 |
| 订单履约 | 订单管理（状态流转、发货）、售后审核（凭证图片 + 拒绝理由必填）、财务对账 |
| 运营管理 | 商务合作审核、价格报价单、新闻管理、工单管理、供应商管理、车间管理 |
| 个人 | 系统设置、个人中心 |

### 🔐 系统超级管理员后台
| 模块 | 页面 |
|------|------|
| 权限体系 | 管理员管理、角色管理（权限树分配）、操作审计日志 |
| 全局 | 站点设置、系统参数 |

---

## 🛠 技术栈

### 后端
| 分类 | 技术 | 版本 |
|------|------|------|
| 语言框架 | Java + Spring Boot | JDK 17 / 2.7.15 |
| ORM | MyBatis-Plus | 3.5.3 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis（Lettuce 连接池） | 6.x / 7.x |
| 图数据库 | Neo4j（Spring Data Neo4j / Bolt Driver） | 4.x / 5.x |
| 对象存储 | MinIO | Latest |
| 认证授权 | JWT（双 Token 体系：买家 + 管理员） | - |
| API 文档 | Knife4j (Swagger 2) | 4.x |
| 安全框架 | Spring Security（permitAll，自定义 HandlerInterceptor 鉴权） | - |
| 支付 | 策略模式 PaymentChannel + 幂等流水表 + HMAC-SHA256 签名验签 | - |

### 前端
| 分类 | 技术 | 版本 |
|------|------|------|
| 语言框架 | Vue 3 + Vite 5 | 3.4 / 5.x |
| 路由 | Vue Router | 4.x |
| UI 组件 | Element Plus（翡翠绿主题定制） | Latest |
| 图表可视化 | ECharts 5（工艺图谱 Graph / Bar / Line / Pie / Gauge） | 5.x |
| 3D 可视化 | Three.js（GLTFLoader / OrbitControls） | Latest |
| 工具库 | Axios、Tailwind CSS | - |

### AI 能力
| 能力 | 实现 |
|------|------|
| 智能客服主链路 | Dify Agent App — SSE streaming（`event: agent_message` 解析） |
| 云端 LLM | deepseek-v3（Dify 接入） |
| 本地 LLM | Ollama + qwen2.5:7b（Dify 双轨模型） |
| 降级兜底 | 本地 FAQ 关键词匹配（优先级：售后 > 联系方式 > 发货 > 产品 > 支付 > 默认） |

### DevOps
| 分类 | 技术 |
|------|------|
| 容器编排 | Docker Compose（6 服务 + healthcheck 顺序启动） |
| 镜像构建 | 后端多阶段 Maven → JRE 17 slim；前端 Node → Nginx alpine |
| 反向代理 | Nginx（前端静态 + /api 反向代理到后端） |

---

## 🏗 系统架构

```
┌───────────────────────────────────────────────────────────────────┐
│                         前端层 (Vue 3)                             │
│  买家端 SPA  │  销售运营后台  │  系统超管后台  │  全屏数据大屏      │
└──────────────────────────┬────────────────────────────────────────┘
                           │ HTTPS / Nginx 80
┌──────────────────────────▼────────────────────────────────────────┐
│                    Spring Boot 后端 (8080)                         │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ Controller  │  │  拦截器链    │  │  AOP 切面    │              │
│  │ (30+ APIs)  │  │ 鉴权→权限→审计│  │ 幂等防重令牌  │              │
│  └──────┬──────┘  └──────────────┘  └──────────────┘              │
│         │                                                          │
│  ┌──────▼──────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   Service   │  │  支付模块    │  │  AI 客服模块  │              │
│  │  策略/状态机 │  │ 策略+幂等+签名│  │ Dify SSE+FAQ │              │
│  └──────┬──────┘  └──────────────┘  └──────────────┘              │
│         │                                                          │
│  ┌──────▼──────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   Mapper    │  │ Repository   │  │   定时任务   │              │
│  │ MyBatis-Plus│  │   (Neo4j)    │  │ 关单/工单调取 │              │
│  └──────┬──────┘  └──────┬───────┘  └──────────────┘              │
└─────────┼────────────────┼────────────────────────────────────────┘
          │                │
┌─────────▼────────────────▼───────┐  ┌────────────────────────────┐
│         MySQL 8.0  (3306)        │  │  Redis 6/7  (6379)          │
│  29 张业务表 / 订单 / 支付流水    │  │  购物车 / 幂等令牌 / 缓存   │
└──────────────────────────────────┘  └────────────────────────────┘
┌──────────────────────────────────┐  ┌────────────────────────────┐
│        Neo4j  (7474 / 7687)      │  │  MinIO  (9000 / 9001)       │
│  工序节点 + NEXT_STEP 工艺边      │  │  商品图片 / 售后凭证        │
└──────────────────────────────────┘  └────────────────────────────┘
```

---

## 🚀 快速启动

### 方式一：Docker Compose 一键部署（推荐给面试官 / 快速体验）

> 需本机已安装 Docker Desktop 4.20+

```bash
# 1. 克隆项目
git clone <your-repo-url>
cd green-chain-procurement

# 2. [可选] 创建 .env 覆盖默认密码（不创建则使用下方默认开发值）
#    MYSQL_ROOT_PASSWORD=your-strong-pwd
#    NEO4J_PASSWORD=your-neo4j-pwd
#    MINIO_ROOT_PASSWORD=your-minio-pwd
#    DIFY_API_KEY=your-dify-app-key   # 不填则 AI 客服自动降级为 FAQ 兜底

# 3. 一键启动（会自动执行 schema.sql + product_seed.sql）
docker-compose up -d

# 4. 等待健康检查通过（约 3~5 分钟，首次构建需拉取镜像较慢）
docker-compose ps

# 5. 访问
#    买家端商城:       http://localhost
#    后端接口文档:     http://localhost:8080/doc.html
#    运营/超管后台:    http://localhost/#/sale-admin  或  /#/sys-admin
#    全屏数据大屏:     http://localhost/#/admin/screen
```

### 方式二：本地分步开发启动

#### 前置依赖
| 依赖 | 版本 | 端口 |
|------|------|------|
| JDK | 17+ | - |
| Maven | 3.8+ | - |
| Node.js | 18+ | - |
| MySQL | 8.0 | 3306 |
| Redis | 6+ | 6379 |
| Neo4j | 4.x / 5.x | 7474 / 7687 |
| MinIO（可选） | Latest | 9000 |

```bash
# ========== 1. 初始化数据库 ==========
# MySQL: 依次执行
mysql -uroot -p < src/main/resources/schema.sql
mysql -uroot -p green_chain < src/main/resources/schema-permission.sql
mysql -uroot -p green_chain < src/main/resources/sql/product_seed.sql
mysql -uroot -p green_chain < src/main/resources/sql/demo_init.sql   # 100w+ 演示数据（可选）

# Neo4j: Browser 执行  http://localhost:7474
cat src/main/resources/neo4j-init.cypher | cypher-shell -u neo4j -p <your-pwd>

# ========== 2. 启动后端 ==========
# application.yml 已支持环境变量覆盖，本地直接用默认值即可
mvn -o spring-boot:run
# 或者打包后运行
mvn -o clean package -DskipTests
java -jar target/green-chain-procurement-1.0.0.jar

# ========== 3. 启动前端 ==========
cd frontend
npm install
npm run dev    # http://localhost:5173
```

### 端口速查表
| 服务 | 端口 | 说明 |
|------|------|------|
| 前端（Nginx） | 80 | Docker 模式下 |
| 前端（Vite Dev） | 5173 | 本地开发模式 |
| Spring Boot 后端 | 8080 | 所有 /api/** 接口 |
| MySQL | 3306 | `green_chain` 库 |
| Redis | 6379 | DB 0（生产新项目用 DB 1） |
| Neo4j Browser | 7474 | Web 控制台 |
| Neo4j Bolt | 7687 | Spring Boot 连接 |
| MinIO API | 9000 | S3 兼容对象存储 |
| MinIO Console | 9001 | 对象存储 Web 控制台 |

---

## 💡 核心亮点详解

### 1. 支付体系：策略模式 + 幂等 + 签名验签 + 状态机

```
发起支付(OrderService)
    │
    ▼
PaymentServiceImpl.createPayment()
    ├─ 参数校验 & 订单状态校验 (pending/paid 未支付才允许)
    ├─ 策略选择 PaymentChannel channel = channelMap.get(defaultChannel)
    │      └─ MockPaymentChannel（当前实现，模拟沙箱网关）
    │      └─ 预留 WechatPaymentChannel / AlipayPaymentChannel（新增实现即可切换）
    ├─ 生成 payment_transaction 流水（唯一索引 ON order_no+channel 防重）
    ├─ channel.prePay() 生成支付链接/二维码
    └─ 返回 PaymentVO（含 payUrl + expireTime）

支付回调（异步）
    │
    ▼
PaymentController.notify()
    ├─ PaySignature.verify() 验签防伪造（HMAC-SHA256 + 密钥）
    ├─ channel.parseNotify() 解析支付结果
    ├─ PaymentTransactionMapper.atomicUpdateStatus() 幂等更新（仅 PROCESSING → SUCCESS）
    ├─ OrderMapper.markPaidByOrderNo() 订单状态机流转：pending → paid
    └─ 售后退款：对称流程，RefundResult + 状态 REVERSED
```

**并发安全设计：**
- 支付流水表 `(order_no, channel, transaction_no)` 唯一索引 → 同笔重复回调 DB 级拒绝
- `UPDATE payment_transaction SET status='SUCCESS' WHERE id=? AND status='PROCESSING'` → 行锁保证幂等
- 订单支付 `UPDATE orders SET status='paid' WHERE order_no=? AND status IN ('pending')` → 条件更新杜绝并发双付

### 2. 订单一致性：防重 + 改价免疫 + 原子库存回补

| 风险场景 | 应对方案 |
|----------|---------|
| 用户重复点击提交 | `@IdempotentToken` 注解 + AOP 切面，Redis SETNX 防重令牌 30s 有效 |
| 前端篡改单价下单 | `ClientOrderController.createOrder()` **禁止使用 requestBody.price**，统一 `productMapper.selectById()` 取 DB 实时价格重算总额 |
| 取消订单时库存回补竞态 | `productMapper.restoreStock(skuId, qty)` → `UPDATE product SET stock = stock + ? WHERE id = ?`，原子 INCR 杜绝读改写 |
| 取消订单状态竞态 | `OrderMapper.cancelIfPendingOrPaid()` → `WHERE status IN ('pending','paid')`，条件 UPDATE 确保只取消未发货订单 |
| 超时未支付关单 | `OrderScheduleTask` Spring 定时任务（cron: `0 */5 * * * ?`），条件 UPDATE 批量关单 + 库存回补 |

### 3. 数字孪生工艺图谱（Neo4j + ECharts + Three.js）

**数据模型（Neo4j）：**
```
(:ProcessNode {processId:'P001', name:'配料', workshop:'电芯制程'})
  -[:NEXT_STEP {duration:1800}]->
(:ProcessNode {processId:'P002', name:'涂布', workshop:'电芯制程'})
  -[:NEXT_STEP ...]-> ...
```

**可视化能力：**
- **多产线切换：** 电芯制程 / PACK 模组 / 电池包总装 三视图独立布局
- **风险定位：** 选择「焊接虚焊/假焊」自动切换对应产线 → 高亮目标工序 + 金色聚光光圈 → 其他节点 38% 淡出
- **产品追溯：** 输入产品编码（如 PRD003 动力电池包），沿 `NEXT_STEP` 箭头方向依序点亮经过的工序；跳过节点时自动补绿色虚线桥接边保证链路视觉连续
- **大屏 3D 展示：** Three.js 加载 `Green-chain.glb` 工厂模型，OrbitControls 交互式漫游

### 4. RBAC 权限 + 三拦截器链 + 操作审计

```
HTTP 请求 → DispatcherServlet
    │
    ├─ AdminAuthInterceptor        ① 鉴权
    │      ├─ JwtUtil.validateAdminToken() 解析 adminId
    │      ├─ 检查账号 status=1（未禁用）→ 否则 401 "账号已被禁用"
    │      └─ PermissionContext.set(adminId, roleIds)  ThreadLocal 传递
    │
    ├─ PermissionInterceptor       ② 细粒度权限
    │      ├─ 读取 Handler 上 @RequiresPermission("order:review")
    │      ├─ 查询 sys_permission 树匹配当前角色
    │      └─ 不匹配 → 403 "无操作权限"（admin 角色直接放行）
    │
    ├─ AdminAuditInterceptor       ③ 审计
    │      ├─ afterCompletion 阶段异步写入 sys_oper_log
    │      ├─ 记录：操作人/IP/模块/请求参数/响应状态/耗时
    │      └─ 审计失败优雅降级 → 仅打 warn 日志不影响主业务
    │
    ▼
Controller → Service → Mapper → DB
    │
    └─ PermissionContext.clear()  finally 块清理 ThreadLocal，避免线程池复用污染
```

### 5. AI 智能客服：Dify SSE 双轨 + FAQ 降级兜底

```
用户消息 → FloatingChat.vue（SSE EventSource）
    │
    ▼
CozeController / DifyService
    │
    ├─ 1. 主链路：Dify Agent App（SSE streaming）
    │      ├─ POST /v1/chat-messages  response_mode:streaming
    │      ├─ HttpURLConnection 逐行解析 SSE
    │      ├─ 匹配 event === "agent_message" → 拼接 answer 字段
    │      ├─ 超时 60s 中断返回
    │      └─ ✅ 正常：转发流回前端  ❌ 空串/异常：→ 降级
    │
    └─ 2. 降级链路：FaqService 本地关键词匹配
           优先级：售后 > 联系方式 > 发货 > 产品 > 支付 > 默认兜底
           └─ 含任一关键词即命中对应模板回复（不依赖网络/AI服务，99.9% 可用）
```

---

## 🗄 数据库（29 张业务表）

### 核心业务链
```
User(用户) → Cart(购物车) + CartItem
     ↓
Order(订单) + OrderItem → PaymentTransaction(支付流水)
     ↓
AfterSale(售后) + evidence(图片凭证，逗号分隔URL)
```

### 权限与审计
```
SysAdmin → SysRole → SysRolePermission → SysPermission（权限树）
     ↓
SysOperLog（操作审计，拦截器自动写入）
```

### 其他
`Product / Category / Carousel / News / BusinessCooperation / Negotiation / Workshop / CustomerServiceRequest / ServiceRequestMessage / ServiceRequestReview / UserAddress / Cart / CartItem / PaymentTransaction`

---

## 🔑 演示账号

| 角色 | 登录入口 | 账号 | 密码 | 权限范围 |
|------|---------|------|------|---------|
| 系统超管 | `/#/sys-admin` | `admin` | `admin123` | 全部功能 + RBAC 配置 + 审计日志 |
| 销售运营 | `/#/sale-admin` | `operator` | `op123456` | 商品/订单/售后/运营（不含用户权限分配） |
| 普通买家 | 顶部登录弹框 | `demo_buyer_001` | `123456` | 购物/下单/售后/工单 |
| 普通买家 | 顶部登录弹框 | `demo_buyer_002` | `123456` | 同上 |

> ⚠️ 演示账号密码默认写入 `src/main/resources/sql/demo_init.sql`，生产部署请务必修改！

---

## 📁 项目目录结构

```
green-chain-procurement/
├── src/main/java/com/greenchain/
│   ├── annotation/          # 自定义注解：@IdempotentToken / @RequiresPermission
│   ├── aspect/              # AOP 切面：IdempotentAspect 防重令牌
│   ├── common/              # 基础组件：Result 统一响应 / BusinessException / 全局异常处理
│   ├── config/              # Spring 配置：跨域/MyBatis-Plus/Redis/Knife4j/Dify/Security/WebMvc
│   ├── controller/          # Controller 层（30+ 接口，Client / Admin 分包）
│   ├── dto/                 # request 请求体 + response 视图对象（VO）
│   ├── entity/              # MyBatis-Plus Entity（29 张表）
│   ├── interceptor/         # 拦截器链：ClientAuth / AdminAuth / Permission / AdminAudit / 订单限流
│   ├── mapper/              # MyBatis-Plus BaseMapper + 自定义原子 SQL 方法
│   ├── payment/             # 支付子包：策略接口 + Mock 实现 + 签名工具 + 配置
│   │   └── dto/             # PrePay/Notify/Refund 请求响应
│   ├── repository/          # Neo4j Repository（工艺节点 CRUD）
│   │   └── impl/            # Neo4j 原生驱动实现 + 内存兜底实现
│   ├── service/             # Service 接口（11+）
│   │   └── impl/            # Service 实现
│   ├── task/                # 定时任务：超时关单 / 工单超时
│   └── util/                # 工具类：JwtUtil / CacheUtil / MinioUtil
│
├── src/main/resources/
│   ├── mapper/              # MyBatis XML 自定义 SQL
│   ├── sql/                 # SQL 脚本：demo_init / product_seed / redis
│   ├── schema.sql           # 数据库完整建表脚本
│   ├── schema-permission.sql# 权限表 & 种子数据
│   ├── neo4j-init.cypher    # Neo4j 工艺节点 & 关系初始化
│   ├── application.yml      # Spring Boot 主配置（环境变量占位脱敏）
│   └── application.example.yml # 配置模板
│
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios 封装（apiModule：client + admin 双轨）
│   │   ├── components/      # 通用组件 + admin 业务组件
│   │   ├── composables/     # Vue 3 Composition：useThreeJS / useMouseControls
│   │   ├── directives/      # 自定义指令：v-permission 按钮级权限
│   │   ├── router/          # Vue Router：四端路由 + 守卫
│   │   ├── utils/           # 工具：spu SKU 转换 / 游客购物车
│   │   ├── views/
│   │   │   ├── admin/       # 18+ 管理页面 + DataScreen 大屏
│   │   │   └── *.vue        # 17+ 买家端页面
│   │   ├── App.vue / main.js / style.css
│   ├── public/              # 静态资源：商品图 / GLB 模型
│   ├── Dockerfile + nginx.conf
│   └── package.json
│
├── docker-compose.yml       # 6 服务一键编排
├── Dockerfile               # 后端多阶段构建
├── pom.xml
└── README.md                # ← 你正在看的文件
```

---

## 📄 API 文档

后端启动后直接访问 Knife4j：**http://localhost:8080/doc.html**

接口分组：
- 🔵 **客户端接口** `/api/client/**` — 买家端（登录、商品、购物车、下单、售后、上传）
- 🟣 **管理端接口** `/api/admin/**` — 运营 + 超管（带 AdminAuthInterceptor + PermissionInterceptor）
- 🟢 **支付接口** `/api/payment/**` — create / notify / simulate-pay
- 🟡 **AI 客服** `/api/coze/**` — SSE 流式对话 + FAQ

---

## 📸 项目截图（TODO：替换为你自己的真实截图）

> 💡 **建议把以下截图放到 `frontend/public/screenshots/` 目录后，替换下方的占位图路径**

| 买家端首页 | 商品详情 | 工艺图谱数字孪生 |
| :--------: | :------: | :---------------: |
| ![首页](https://placehold.co/600x340/0d9488/ffffff?text=Buyer+Home+Page) | ![详情](https://placehold.co/600x340/06b6d4/ffffff?text=Product+Detail) | ![图谱](https://placehold.co/600x340/059669/ffffff?text=Process+Graph+Neo4j) |

| 数据大屏 | 管理仪表盘 | 售后审核 |
| :------: | :--------: | :------: |
| ![大屏](https://placehold.co/600x340/1e293b/10b981?text=Full+Screen+DataScreen) | ![仪表盘](https://placehold.co/600x340/f59e0b/111827?text=Admin+Dashboard) | ![售后](https://placehold.co/600x340/8b5cf6/ffffff?text=AfterSale+Review) |

---

## 🧪 Postman 验证要点（面试官最常问）

**订单主流程（按序调用）：**
1. `POST /api/client/auth/register` 注册 → 2. `/login` 获取 token → 3. `/cart/add` 加购 → 4. `/order/create` 下单（拿 orderNo）→ 5. `/payment/create` 发起支付 → 6. `/payment/simulate-pay?orderNo=xxx` 模拟支付成功 → 7. `/client/order/detail` 验证订单状态 paid → 8. `/client/after-sale/apply` 申请售后 → 9. `/admin/after-sale/review` 运营审核 → 10. 财务对账退款

> 支付回调接口 `/api/payment/notify` 的签名头 `X-Signature` = `HMAC-SHA256(secretKey, body)`，可 Postman Pre-request 脚本生成。

---

## 📜 License

[MIT License](LICENSE) — 可自由使用、修改、分发，保留版权声明即可。

---

<p align="center">
  <b>如果这个项目对你的求职/学习有帮助，请给个 ⭐ Star，感谢支持！</b>
</p>
