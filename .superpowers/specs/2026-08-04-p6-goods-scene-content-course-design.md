# P6 商品域 + 场景域 + 内容域 + 课程域 - 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老 P6 阶段（goods 5 表 + scene 5 表 + content 5 表 + course 3 表 = 18 表）
> **依据**：`docs/08项目计划书.md` §2.3 P6、P0-P5 已完成基础
> **前置**：P3 机构域（SKU 关联 park）、P4 权益域（SKU 关联 equity_template）

---

## 一、范围

四域是权益的服务标的（权益兑换商品/场景/内容/课程）：

| 序号 | 域 | 表数 | 核心 |
|------|----|----|------|
| 1 | **商品域 goods_** | 5 | goods_info SPU（4 类型）+ 4 类 SKU（equity/scene/course/sojourn） |
| 2 | **场景域 scene_** | 5 | scene_info 场景（8 类）+ item 项目 + item_price 定价 + schedule 排期 + resource 资源 |
| 3 | **内容域 content_** | 5 | content_info 内容（5 类型/审核流）+ category 分类 + media 多媒体 + record_share/read 记录 |
| 4 | **课程域 course_** | 3 | course_info 课程 + lecturer 讲师 + record_learn 学习记录 |

### P6 本阶段（必做）
- 四域 18 表 Service+Controller CRUD
- 商品 SPU + 4 类 SKU 管理（goodsType 区分，SKU 关联各自资源）
- 场景全生命周期（创建→排期→预约容量→资源冲突检测）
- 内容审核流（草稿/待审/通过/拒绝/下线）+ 阅读统计（UV/PV）
- 课程管理 + 学习记录

### P6 后置（不在本阶段）
- Admin 前端页面（P8）
- 内容按月分区实际建表（P6 content_record_read 设计有分区，但仅记录，分区维护后置）
- 商品库存扣减的分布式事务（P7 订单域联动）
- 课程学习进度追踪的复杂算法（P6 仅记录）

---

## 二、全局约束（实现者必读）

### 2.1 包结构
- 商品：`com.dayan.goods.{controller.admin,service,service.impl,dto,vo,enums}`
- 场景：`com.dayan.scene.{...}`
- 内容：`com.dayan.content.{...}`
- 课程：`com.dayan.course.{...}`
- Controller 仅 `controller/admin`

### 2.2 主键策略（P6 基础设施，控制者已改）
- **goods/scene/course 全是平台共享表（AUTO_INCREMENT）**——Entity 保持 IdType.AUTO ✓
- **content**：content_info/category/media 共享（AUTO ✓），content_record_share/record_read 分片表 → **改 ASSIGN_ID**
- 四域 Entity 的 @TableId 当前状态需核实，分片表的改 ASSIGN_ID

### 2.3 租户隔离（P6 基础设施，控制者已改）
- **goods_/scene_/content_/course_ 加入 DayanTenantHandler DEFAULT_IGNORE_PREFIXES**
- 理由：这四域是平台共享资源（Admin 全局管理），非渠道内数据；渠道差异化通过 channel_config_*（P2 已做）和 scene_item_price 的 channel 维度定价体现

### 2.4 跨模块依赖（只读 View 模式）
- goods SKU 关联 equity_template/scene_info/course_info/park_info，但 goods 模块不依赖这些模块
- 用只读 View Entity（`@TableName` 指向目标表），参考 P3 park 的 SupplierInfoView / P5 service 的 ButlerInfoView 模式
- **P6 简化**：SKU 的关联校验（如 equity_template_code 存在性）改为**弱校验**（仅格式校验，不跨模块查存在性），或建 View 查。实现者按需建 View，不强求每个关联都查。

### 2.5 编码生成（SequenceProvider）
- goods_code: `"GD" + format(%05d, seq)`（BusinessCode.GOODS="GD"）
- scene_code: `"SC" + format(%05d, seq)`（BusinessCode.SCENE="SC"）
- content_code: `"CT" + format(%05d, seq)`（BusinessCode.CONTENT="CT"）
- course_code: `"CR" + format(%05d, seq)`（BusinessCode.COURSE="CR"）
- 注入 `private final SequenceProvider sequenceProvider;`

### 2.6 统一返回 / 异常 / DTO
- `R<T>`/`R<PageResult<T>>`，`BusinessException`+`ErrorCode`，手动 BeanUtils.copyProperties
- Service `@RequiredArgsConstructor`，核心操作 `@Transactional`
- Controller 相对路径（`/goods/info` 等），靠 context-path=/admin-api 拼接
- 不加 @SaCheckPermission，加 swagger @Tag/@Operation，pom.xml 不改

### 2.7 参考实现
- `dayan-module-channel` Service+Controller（P2 标准模式）
- `dayan-module-park/entity/SupplierInfoView.java`（跨模块只读 View，P3）
- `dayan-common-core/code/SequenceProvider`（编码生成）

---

## 三、商品域设计（5 表）

### 3.1 goods_info（商品 SPU）
- CRUD + goods_code(GD+5) 唯一
- goodsType: 1权益/2场景/3课程/4旅居（4 类型决定关联哪类 SKU）
- goodsStatus: 0下架/1上架；auditStatus: 0待审/1通过/2拒绝
- 上下架管理接口

### 3.2 goods_sku_equity（权益 SKU）
- 按 goodsCode CRUD
- 关联 equityTemplateCode（弱校验或 View 查）
- equityLevel 分等级配置 + stock 库存

### 3.3 goods_sku_scene（场景 SKU）
- 按 goodsCode CRUD
- 关联 sceneCode + 时段定价 + 批量折扣

### 3.4 goods_sku_course（课程 SKU）
- 按 goodsCode CRUD
- 关联 courseCode + maxStudents 学员上限

### 3.5 goods_sku_sojourn（旅居 SKU）
- 按 goodsCode CRUD
- 关联 parkCode + roomTypeCode + 时长

---

## 四、场景域设计（5 表）

### 4.1 scene_info（场景信息）
- CRUD + scene_code(SC+5) 唯一 + sceneName 唯一
- sceneType: 8 类场景
- 关联 parkCode（弱校验或 View）
- sceneStatus/auditStatus

### 4.2 scene_item（项目明细）
- 按 sceneCode CRUD
- 多项目组成场景，itemCode 同场景内唯一

### 4.3 scene_item_price（项目定价）
- 按 sceneCode/itemCode CRUD
- 渠道差异化定价（channelCode 维度）
- 批量采购折扣（quantity 阶梯）

### 4.4 scene_schedule（日程安排）
- 按 sceneCode CRUD
- scheduleDate + timeSlot + maxPerson/currentPerson
- **currentPerson ≤ maxPerson 校验**
- status: 1开放/2已约满/3关闭

### 4.5 scene_resource（场景资源）
- 按 sceneCode CRUD
- **资源冲突检测**：同资源同时间段不重叠（应用层校验）

---

## 五、内容域设计（5 表）

### 5.1 content_info（内容信息）
- CRUD + content_code(CT+5) 唯一 + title 唯一
- contentType: 1文章/2视频/3图片集/4专题/5问答
- contentStatus: 0草稿/1待审/2通过/3拒绝/4下线（多状态流转）
- 审核接口：待审→通过/拒绝，通过→下线
- SEO 配置（如有字段）

### 5.2 content_category（内容分类）
- CRUD，树形或平铺（按 categoryName）

### 5.3 content_media（多媒体资源）
- 按 contentCode CRUD

### 5.4 content_record_share（分享记录，分片表 ASSIGN_ID）
- 按 contentCode CRUD
- trace_id 归因

### 5.5 content_record_read（阅读记录，分片表 ASSIGN_ID）
- 按 contentCode 查询统计（UV/PV）
- 记录阅读事件

---

## 六、课程域设计（3 表）

### 6.1 course_info（课程信息）
- CRUD + course_code(CR+5) 唯一
- courseType: 4 类型
- courseStatus: 0下架/1上架
- maxStudents/currentStudents（current ≤ max）

### 6.2 course_lecturer（课程讲师）
- CRUD，lecturerCode 关联

### 6.3 course_record_learn（学习记录，分片表）
- 按 courseCode/clientCode CRUD
- 学习进度记录

---

## 七、API 路径规范

| 域 | 路径 |
|----|------|
| 商品 | `/goods/info` `/goods/sku-equity` `/goods/sku-scene` `/goods/sku-course` `/goods/sku-sojourn` |
| 场景 | `/scene/info` `/scene/item` `/scene/item-price` `/scene/schedule` `/scene/resource` |
| 内容 | `/content/info`（+/audit /publish/offline）`/content/category` `/content/media` `/content/record-share` `/content/record-read` |
| 课程 | `/course/info` `/course/lecturer` `/course/record-learn` |

标准动作：`/page /list /{code或id} POST / PUT / DELETE /{code或id}`。

---

## 八、验收标准

| 维度 | 标准 |
|------|------|
| 商品 | SPU CRUD + 4 类 SKU + goods_code 唯一 + 上下架 |
| 场景 | 5 表 CRUD + 排期容量校验 + 资源冲突检测 |
| 内容 | 5 表 + 审核流 + title 唯一 + 阅读统计 |
| 课程 | 3 表 + course_code 唯一 + current≤max |
| 分片表 | content 2 分片表 ASSIGN_ID |
| 编译 | 41 模块 BUILD SUCCESS |

---

## 九、任务拆分与执行

四域在不同模块，完全独立，可四路并行：

| 任务 | 内容 | 可并行 |
|------|------|--------|
| **P6-A** | 商品域 5 表（SPU+4 SKU） | ✅ |
| **P6-B** | 场景域 5 表（排期容量+资源冲突） | ✅ |
| **P6-C** | 内容域 5 表（审核流+阅读统计） | ✅ |
| **P6-D** | 课程域 3 表 | ✅ |

**执行**：控制者先做基础设施（租户忽略 + content ASSIGN_ID），再四路并行分派子智能体。
