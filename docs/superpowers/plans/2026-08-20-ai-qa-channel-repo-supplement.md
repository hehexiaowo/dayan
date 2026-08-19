# 你问我答渠道补充知识库——实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** admin 全局绑定知识库的 aichat 人物，渠道可在控制台按人物补充自己（含后代渠道）的知识库；agent 聊天按"全局 ∪ 渠道补充"合并检索，每个渠道的有效知识库不同。

**架构：** 新增 `tool_channel_repo_bind` 表存渠道补充（tool_code + channel_code + repo_id）；`ToolChannelRepoBindService` 负责读写与合并；`SystemKnowledgeRepoService` 新增 `requireRepoVisibleForPersona`（平台库对渠道放行，仅 aichat 路径）与 `listChannelScopeRepos`（自己+后代渠道的库）；渠道控制台新增"问答人物"页；agent 端零前端改动。

**技术栈：** Spring Boot + MyBatis-Plus（server，Maven 多模块）、Vue3 + Element Plus + Vite（dayan-channel）、MySQL 8（容器 dayan-mysql，root/root123）。

**测试说明（TDD 适配）：** 本仓库无任何测试基础设施（server 无 `src/test`、前端无 vitest/jest）。本计划以"编译验证 + 关键路径人工验证"代替单测：server 用 `mvn -pl <starter> -am compile`，前端用 `vue-tsc --noEmit && vite build`。每个任务末尾都有验证步骤与 commit。

**执行前置：** 运行中的 MySQL 容器 `dayan-mysql`（密码默认 `root123`）；种子与迁移 SQL 需手动对运行实例执行（`docker exec`），db/migration 目录仅用于全新初始化。

**规格：** `docs/superpowers/specs/2026-08-20-ai-qa-channel-repo-supplement-design.md`

---

### 任务 1：DB 迁移——新表 `tool_channel_repo_bind`

**文件：**
- 创建：`db/migration/91_tool_channel_repo_bind.sql`
- 修改：`db/migration/seed/channel_permission_seed.sql`（追加）
- 修改：`db/migration/seed/menu_seed_channel.sql`（追加）

- [ ] **步骤 1：创建迁移文件**

创建 `db/migration/91_tool_channel_repo_bind.sql`（必须以 `SET NAMES utf8mb4;` 开头，遵守目录规约）：

```sql
SET NAMES utf8mb4;
-- =====================================================================
-- 91_tool_channel_repo_bind.sql  渠道问答人物补充知识库绑定
--
-- 你问我答人物（tool_info 的 aichat 实例）知识库两层模型：
--   admin 全局绑定存 tool_info.config_json.repoIds；
--   渠道补充存本表（按人物分别补充，并集生效）。
-- 运行时有效库 = 全局 repoIds ∪ 本表查询结果（去重保序）。
-- =====================================================================
CREATE TABLE `tool_channel_repo_bind` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tool_code` VARCHAR(50) NOT NULL COMMENT '问答人物编码（tool_info.tool_code）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '补充方渠道编码（ContextHolder 注入）',
  `repo_id` BIGINT NOT NULL COMMENT '补充的知识库 ID（system_knowledge_repo.id）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1=已删除，0=未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_channel_repo` (`tool_code`, `channel_code`, `repo_id`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='渠道问答人物补充知识库绑定';
```

- [ ] **步骤 2：对运行中的 MySQL 执行迁移**

```bash
docker exec -i dayan-mysql mysql -uroot -proot123 dayan < db/migration/91_tool_channel_repo_bind.sql
```

预期：无报错输出。验证：

```bash
docker exec -i dayan-mysql mysql -uroot -proot123 dayan -e "SHOW CREATE TABLE tool_channel_repo_bind\G"
```

- [ ] **步骤 3：追加权限种子**

在 `db/migration/seed/channel_permission_seed.sql` 末尾追加（幂等，sort_order 210/211 接在知识仓库 200-207 之后）：

```sql
-- ========== 增量：问答人物（渠道补充知识库）权限码 ==========
-- 对应 ChannelToolAichatController（/channel-api/tools/aichat/*，dayan-module-tool）。
INSERT INTO `channel_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`, `sort_order`, `status`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('channel:tool:aichat:view',   '问答人物查看',     'channel:system', 3, '/channel-api/tools/aichat/**',                 'GET',  210, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:tool:aichat:update', '问答人物补充库',   'channel:system', 3, '/channel-api/tools/aichat/personas/*/repos',   'PUT',  211, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
```

- [ ] **步骤 4：追加菜单种子**

在 `db/migration/seed/menu_seed_channel.sql` 末尾追加（挂在 `channel_system` 目录下，sort_order 50 接在知识仓库 40 之后；icon 用 Element Plus 已有图标名）：

```sql
-- ==================== 问答人物（系统管理 → 问答人物，渠道补充知识库）====================
-- 对应 channel 端落地页 views/tool/aichat/index.vue + ChannelToolAichatController。
INSERT INTO system_menu
(menu_code, menu_name, parent_code, menu_type, path, component, permission_code,
 icon, sort_order, is_visible, is_external, is_cache, domain_type, status,
 created_at, updated_at, creator, updater, deleted, deleted_at)
VALUES
('channel_system_tool_aichat', '问答人物', 'channel_system', 2, '/system/tool-aichat', 'tool/aichat/index', 'channel:tool:aichat:view',
 'ChatDotRound', 50, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
```

- [ ] **步骤 5：对运行中的 MySQL 执行种子**

```bash
docker exec -i dayan-mysql mysql -uroot -proot123 dayan < db/migration/seed/channel_permission_seed.sql
docker exec -i dayan-mysql mysql -uroot -proot123 dayan < db/migration/seed/menu_seed_channel.sql
```

验证：

```bash
docker exec -i dayan-mysql mysql -uroot -proot123 dayan -e "SELECT permission_code FROM channel_permission WHERE permission_code LIKE 'channel:tool:aichat:%'; SELECT menu_code FROM system_menu WHERE menu_code='channel_system_tool_aichat';"
```

预期：各返回 2 行 / 1 行。

- [ ] **步骤 6：Commit**

```bash
git add db/migration/91_tool_channel_repo_bind.sql db/migration/seed/channel_permission_seed.sql db/migration/seed/menu_seed_channel.sql
git commit -m "feat(db): 渠道问答人物补充知识库——tool_channel_repo_bind 表 + 菜单/权限种子"
```

---

### 任务 2：System 模块——渠道可见性放行与补充可选库范围

**文件：**
- 修改：`dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemKnowledgeRepoService.java`
- 修改：`dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/impl/SystemKnowledgeRepoServiceImpl.java`

- [ ] **步骤 1：接口增加两个方法**

`SystemKnowledgeRepoService.java` 中 `requireRepoVisible(Long id)` 声明（约 112 行）之后追加：

```java
    /**
     * 人物绑定路径的可见性校验（aichat 聊天专用）：
     * 平台库对渠道端放行（admin 全局绑定生效），渠道库维持归属/祖先/后代校验。
     * 无渠道上下文（admin 端）全部放行。
     */
    SystemKnowledgeRepo requireRepoVisibleForPersona(Long id);

    /** 渠道可补充的知识库：本渠道 + 全部后代渠道名下的渠道库（不含平台库），用于渠道问答人物补充下拉与保存校验 */
    List<SystemKnowledgeRepoVO> listChannelScopeRepos(String channelCode);
```

- [ ] **步骤 2：抽取渠道范围算法（getRepoTree 复用）**

`SystemKnowledgeRepoServiceImpl.java` 的 `getRepoTree(String rootChannelCode)`（约 136 行）中，把 scope 收集段（从 `// 收集 root 及其全部后代` 到 `scope.add(c.getChannelCode()); }` 结束的 for 循环，即 144-151 行）抽取为私有方法，getRepoTree 改为调用它。抽取后：

```java
    /**
     * 渠道范围：root 及其全部后代（ancestors 链包含 root 的渠道；root 为空 = 全渠道树）。
     * getRepoTree 与 listChannelScopeRepos 共用。
     */
    private Set<String> resolveChannelScope(String rootChannelCode) {
        List<ChannelInfoLight> channels = channelInfoLightMapper.selectAll();
        Set<String> scope = new HashSet<>();
        for (ChannelInfoLight c : channels) {
            if (StrUtil.isBlank(rootChannelCode) || rootChannelCode.equals(c.getChannelCode())
                    || (c.getAncestors() != null && c.getAncestors().contains(rootChannelCode))) {
                scope.add(c.getChannelCode());
            }
        }
        return scope;
    }
```

`getRepoTree` 中 136-151 行替换为：

```java
    public List<SystemKnowledgeRepoTreeNodeVO> getRepoTree(String rootChannelCode) {
        List<ChannelInfoLight> channels = channelInfoLightMapper.selectAll();
        if (channels.isEmpty()) {
            return List.of();
        }
        Map<String, ChannelInfoLight> byCode = channels.stream()
                .collect(Collectors.toMap(ChannelInfoLight::getChannelCode, Function.identity(), (a, b) -> a));
        // 收集 root 及其全部后代（ancestors 链包含 root）；root 为空 = 全渠道树
        Set<String> scope = resolveChannelScope(rootChannelCode);
        // 继承解析需要祖先链上的仓库（scope 渠道的 ancestors 一并纳入查询范围）
        Set<String> queryCodes = new HashSet<>(scope);
        for (String code : scope) {
            ChannelInfoLight c = byCode.get(code);
            if (c != null && StrUtil.isNotBlank(c.getAncestors())) {
                for (String anc : c.getAncestors().split(",")) {
                    if (StrUtil.isNotBlank(anc)) {
                        queryCodes.add(anc);
                    }
                }
            }
        }
```

（`resolveChannelScope` 需要 `import java.util.Set;`——文件已 import，见下方步骤 3 确认；原 137-151 行的 `channels` 局部变量与 `byCode` 保持不变，原 152-163 行 queryCodes 循环原样保留。）

- [ ] **步骤 3：实现两个新方法**

在 `SystemKnowledgeRepoServiceImpl.java` 的 `requireRepoVisible(Long id)` 方法（约 227-270 行）之后追加：

```java
    @Override
    public SystemKnowledgeRepo requireRepoVisibleForPersona(Long id) {
        SystemKnowledgeRepo repo = knowledgeRepoMapper.selectByIdIgnoreTenant(id);
        if (repo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        String currentCode = ContextHolder.getChannelCode();
        if (StrUtil.isBlank(currentCode)) {
            // 未绑定渠道上下文（admin 端）放行
            return repo;
        }
        // 平台库（channel_code=null）对渠道端放行：admin 全局绑定的人物流量在所有渠道可用
        if (StrUtil.isBlank(repo.getChannelCode())) {
            return repo;
        }
        // 渠道库维持归属/祖先/后代校验
        return requireRepoVisible(id);
    }

    @Override
    public List<SystemKnowledgeRepoVO> listChannelScopeRepos(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return List.of();
        }
        Set<String> scope = resolveChannelScope(channelCode);
        if (scope.isEmpty()) {
            return List.of();
        }
        return knowledgeRepoMapper.selectByChannelCodes(scope).stream()
                .filter(r -> r.getRepoType() != null && r.getRepoType() == TYPE_CHANNEL
                        && r.getChannelCode() != null && scope.contains(r.getChannelCode()))
                .map(this::toVO)
                .toList();
    }
```

确认 import：文件已 import `java.util.Set`、`java.util.HashSet`（getRepoTree 原代码已用）、`java.util.List`、`com.dayan.common.mybatis.context.ContextHolder`、`com.dayan.system.vo.SystemKnowledgeRepoVO`——如缺任一则补齐。

- [ ] **步骤 4：编译验证**

```bash
cd dayan-server && mvn -q -pl dayan-modules/dayan-module-system -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **步骤 5：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemKnowledgeRepoService.java dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/impl/SystemKnowledgeRepoServiceImpl.java
git commit -m "feat(system): 知识库可见性放行（人物绑定路径平台库可用）+ 渠道补充可选库范围查询"
```

---

### 任务 3：Tool 模块——渠道补充绑定实体与读写服务

**文件：**
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/entity/ToolChannelRepoBind.java`
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/mapper/ToolChannelRepoBindMapper.java`
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/ToolChannelRepoBindService.java`
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolChannelRepoBindServiceImpl.java`

- [ ] **步骤 1：创建实体**

`ToolChannelRepoBind.java`（继承 BaseEntity 复用审计字段；表结构见任务 1）：

```java
package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_channel_repo_bind 对应实体（渠道问答人物补充知识库绑定）。
 *
 * <p>你问我答人物知识库两层模型：admin 全局绑定存 tool_info.config_json.repoIds，
 * 渠道补充存本表；运行时有效库 = 全局 ∪ 渠道补充（并集去重）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_channel_repo_bind")
public class ToolChannelRepoBind extends BaseEntity {

    /** 问答人物编码（tool_info.tool_code，TL 前缀） */
    private String toolCode;

    /** 补充方渠道编码（服务端 ContextHolder 注入） */
    private String channelCode;

    /** 补充的知识库 ID（system_knowledge_repo.id） */
    private Long repoId;
}
```

- [ ] **步骤 2：创建 Mapper**

`ToolChannelRepoBindMapper.java`：

```java
package com.dayan.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.tool.entity.ToolChannelRepoBind;
import org.apache.ibatis.annotations.Mapper;

/** 渠道问答人物补充知识库绑定 Mapper */
@Mapper
public interface ToolChannelRepoBindMapper extends BaseMapper<ToolChannelRepoBind> {
}
```

- [ ] **步骤 3：创建 Service 接口**

`ToolChannelRepoBindService.java`：

```java
package com.dayan.tool.service;

import java.util.List;

/**
 * 渠道问答人物补充知识库绑定服务。
 *
 * <p>合并规则：有效知识库 = admin 全局 repoIds ∪ 渠道补充 repoIds（去重保序）。
 * 无渠道上下文时退化为仅全局。</p>
 */
public interface ToolChannelRepoBindService {

    /** 某渠道对某人物补充的知识库 ID 列表（无则空列表） */
    List<Long> listRepoIds(String toolCode, String channelCode);

    /** 合并有效知识库：全局 + 当前渠道补充（去重保序；无渠道上下文退化为仅全局） */
    List<Long> mergeRepoIds(String toolCode, List<Long> globalRepoIds);

    /**
     * 保存渠道补充（全量替换：删除旧行后插入新集合）。
     * 校验：每个 repo 必须属于该渠道自己或后代渠道名下（复用 SystemKnowledgeRepoService.listChannelScopeRepos），
     * 范围外抛 BusinessException。
     */
    void saveChannelRepos(String toolCode, String channelCode, List<Long> repoIds);
}
```

- [ ] **步骤 4：创建 Service 实现**

`ToolChannelRepoBindServiceImpl.java`（校验 repo 归属依赖 system 模块的 `SystemKnowledgeRepoService`，tool 模块 pom 已依赖 system 模块；**不校验人物存在性**——由控制器层 `getQaPersona` 负责，避免 ToolInfoService ↔ 本服务构造器循环依赖）：

```java
package com.dayan.tool.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import com.dayan.tool.entity.ToolChannelRepoBind;
import com.dayan.tool.mapper.ToolChannelRepoBindMapper;
import com.dayan.tool.service.ToolChannelRepoBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 渠道问答人物补充知识库绑定服务实现 */
@Service
@RequiredArgsConstructor
public class ToolChannelRepoBindServiceImpl implements ToolChannelRepoBindService {

    private final ToolChannelRepoBindMapper bindMapper;
    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Override
    public List<Long> listRepoIds(String toolCode, String channelCode) {
        if (StrUtil.isBlank(toolCode) || StrUtil.isBlank(channelCode)) {
            return List.of();
        }
        return bindMapper.selectList(new LambdaQueryWrapper<ToolChannelRepoBind>()
                        .eq(ToolChannelRepoBind::getToolCode, toolCode)
                        .eq(ToolChannelRepoBind::getChannelCode, channelCode)
                        .orderByAsc(ToolChannelRepoBind::getId))
                .stream().map(ToolChannelRepoBind::getRepoId).toList();
    }

    @Override
    public List<Long> mergeRepoIds(String toolCode, List<Long> globalRepoIds) {
        Set<Long> merged = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(globalRepoIds)) {
            merged.addAll(globalRepoIds);
        }
        String channelCode = ContextHolder.getChannelCode();
        if (StrUtil.isNotBlank(channelCode)) {
            merged.addAll(listRepoIds(toolCode, channelCode));
        }
        return List.copyOf(merged);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChannelRepos(String toolCode, String channelCode, List<Long> repoIds) {
        if (StrUtil.isBlank(toolCode) || StrUtil.isBlank(channelCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "人物编码与渠道编码不能为空");
        }
        List<Long> targets = repoIds == null ? List.of() : repoIds.stream().distinct().toList();
        // 归属校验：仅允许本渠道 + 后代渠道名下的渠道库
        Set<Long> allowed = knowledgeRepoService.listChannelScopeRepos(channelCode).stream()
                .map(SystemKnowledgeRepoVO::getId).collect(Collectors.toSet());
        for (Long repoId : targets) {
            if (repoId == null || !allowed.contains(repoId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不在可补充范围内: " + repoId);
            }
        }
        bindMapper.delete(new LambdaQueryWrapper<ToolChannelRepoBind>()
                .eq(ToolChannelRepoBind::getToolCode, toolCode)
                .eq(ToolChannelRepoBind::getChannelCode, channelCode));
        for (Long repoId : targets) {
            ToolChannelRepoBind bind = new ToolChannelRepoBind();
            bind.setToolCode(toolCode);
            bind.setChannelCode(channelCode);
            bind.setRepoId(repoId);
            bindMapper.insert(bind);
        }
    }
}
```

- [ ] **步骤 5：编译验证**

```bash
cd dayan-server && mvn -q -pl dayan-modules/dayan-module-tool -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/entity/ToolChannelRepoBind.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/mapper/ToolChannelRepoBindMapper.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/ToolChannelRepoBindService.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolChannelRepoBindServiceImpl.java
git commit -m "feat(tool): 渠道问答人物补充知识库绑定服务——读写/合并/归属校验"
```

---

### 任务 4：Tool 模块——聊天检索合并与 agent 人物列表合并

**文件：**
- 修改：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolAichatChatServiceImpl.java`
- 修改：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolInfoServiceImpl.java`

- [ ] **步骤 1：聊天跨库检索改用合并 + 新可见性校验**

`ToolAichatChatServiceImpl.java`：
1. 类字段区（`private final ToolAichatStreamService ...` 附近，类注解为 `@RequiredArgsConstructor`）追加：

```java
    private final ToolChannelRepoBindService bindService;
```

2. `retrieveCitations` 方法（约 204 行）两处替换：

```java
        List<Long> repoIds = bindService.mergeRepoIds(persona.getToolCode(), persona.getRepoIds());
        for (Long repoId : repoIds) {
            try {
                knowledgeRepoService.requireRepoVisibleForPersona(repoId);  // 人物绑定路径：平台库放行 + 渠道库归属校验
```

（原代码：`List<Long> repoIds = persona.getRepoIds() == null ? List.of() : persona.getRepoIds();` 与 `knowledgeRepoService.requireRepoVisible(repoId);`）

3. 补充 import：`import com.dayan.tool.service.ToolChannelRepoBindService;`

- [ ] **步骤 2：agent 人物列表返回渠道合并后的 repoIds**

`ToolInfoServiceImpl.java`：
1. 字段区（`@RequiredArgsConstructor` 构造注入）追加：

```java
    private final ToolChannelRepoBindService bindService;
```

2. `listQaPersonas()`（79-85 行）的 stream 链改为合并后返回：

```java
    @Override
    public List<ToolAichatPersonaVO> listQaPersonas() {
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getToolType, ToolType.AI_QA)
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toPersona)
                .map(p -> {
                    p.setRepoIds(bindService.mergeRepoIds(p.getToolCode(), p.getRepoIds()));
                    return p;
                })
                .toList();
    }
```

3. 从 `toPersona`（252 行起）抽取 config_json 的 repoIds 解析为私有方法，`toPersona` 与 `listQaPersonas` 共用（`getQaPersona` 保持原样返回原始全局 repoIds，任务 5 的渠道列表与校验依赖它）：

```java
    /** 从 config_json 解析全局绑定的知识库 ID（缺失/非法按空处理） */
    private List<Long> parseRepoIds(ToolInfo tool) {
        if (StrUtil.isNotBlank(tool.getConfigJson())) {
            JSONObject cfg = JSONUtil.parseObj(tool.getConfigJson());
            if (cfg.getJSONArray("repoIds") != null) {
                return cfg.getJSONArray("repoIds").toList(Long.class);
            }
        }
        return List.of();
    }
```

`toPersona` 中对应段（原 `if (cfg.getJSONArray("repoIds") != null) { vo.setRepoIds(...); }`）替换为：

```java
            vo.setRepoIds(parseRepoIds(tool));
```

4. 补充 import：`import com.dayan.tool.service.ToolChannelRepoBindService;`

- [ ] **步骤 3：编译验证**

```bash
cd dayan-server && mvn -q -pl dayan-modules/dayan-module-tool -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolAichatChatServiceImpl.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolInfoServiceImpl.java
git commit -m "feat(tool): aichat 聊天按全局∪渠道补充合并检索，agent 人物列表返回渠道有效知识库"
```

---

### 任务 5：Tool 模块——渠道端接口（人物列表 / 可选库 / 保存补充）

**文件：**
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/dto/ToolChannelRepoBindDTO.java`
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/vo/ToolChannelPersonaVO.java`
- 创建：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/controller/channel/ChannelToolAichatController.java`
- 修改：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/ToolInfoService.java`
- 修改：`dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolInfoServiceImpl.java`

- [ ] **步骤 1：创建 DTO**

`ToolChannelRepoBindDTO.java`：

```java
package com.dayan.tool.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 渠道保存问答人物补充知识库请求（全量替换；空数组 = 清空补充） */
@Data
public class ToolChannelRepoBindDTO {

    @Size(max = 100, message = "补充知识库数量过多")
    private List<Long> repoIds;
}
```

- [ ] **步骤 2：创建 VO**

`ToolChannelPersonaVO.java`（渠道控制台人物列表：全局库与渠道补充分开展示）：

```java
package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

/** 渠道端问答人物（含 admin 全局库与本渠道补充库，供控制台分别展示） */
@Data
public class ToolChannelPersonaVO {

    /** 工具实例编码（TL 前缀） */
    private String toolCode;

    /** 人物名称（= tool_info.tool_name） */
    private String personaName;

    /** 工具简介 */
    private String toolDesc;

    /** admin 全局绑定的知识库 ID（config_json.repoIds，只读） */
    private List<Long> globalRepoIds;

    /** 本渠道补充的知识库 ID（tool_channel_repo_bind，可编辑） */
    private List<Long> channelRepoIds;
}
```

- [ ] **步骤 3：ToolInfoService 接口增加方法**

`ToolInfoService.java` 中 `getQaPersona` 声明（约 38 行）之后追加：

```java
    /** 渠道端问答人物列表（启用中 aichat 实例；含全局库与本渠道补充库） */
    List<ToolChannelPersonaVO> listChannelPersonas(String channelCode);
```

补充 import：`import com.dayan.tool.vo.ToolChannelPersonaVO;`

- [ ] **步骤 4：ToolInfoServiceImpl 实现 listChannelPersonas**

在 `listQaPersonas()` 方法之后追加（类已注入 `bindService`，见任务 4 步骤 2）：

```java
    @Override
    public List<ToolChannelPersonaVO> listChannelPersonas(String channelCode) {
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getToolType, ToolType.AI_QA)
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(tool -> {
                    ToolChannelPersonaVO vo = new ToolChannelPersonaVO();
                    vo.setToolCode(tool.getToolCode());
                    vo.setPersonaName(tool.getToolName());
                    vo.setToolDesc(tool.getToolDesc());
                    vo.setGlobalRepoIds(parseRepoIds(tool));
                    vo.setChannelRepoIds(bindService.listRepoIds(tool.getToolCode(), channelCode));
                    return vo;
                }).toList();
    }
```

补充 import：`import com.dayan.tool.vo.ToolChannelPersonaVO;`

- [ ] **步骤 5：创建渠道控制器**

`ChannelToolAichatController.java`（包 `com.dayan.tool.controller.channel`；dayan-channel starter 组件扫描 `com.dayan` 且排除正则不含 `channel` 包，启动后自动生效；路径前缀 `/channel-api` 由 starter context-path 拼接）：

```java
package com.dayan.tool.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import com.dayan.tool.dto.ToolChannelRepoBindDTO;
import com.dayan.tool.service.ToolChannelRepoBindService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolChannelPersonaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端 AI 问答人物接口（渠道补充知识库）。
 *
 * <p>路径 {@code /tools/aichat}（dayan-channel starter context-path 拼接为 {@code /channel-api/tools/aichat/*}）。
 *
 * <p>渠道隔离：channelCode 一律从 {@link ContextHolder} 强制注入，不接收前端参数；
 * 补充范围仅限本渠道 + 后代渠道名下的渠道库（{@code listChannelScopeRepos} 校验）。</p>
 */
@Tag(name = "Channel AI 问答人物")
@RestController
@RequestMapping("/tools/aichat")
@RequiredArgsConstructor
public class ChannelToolAichatController {

    private final ToolInfoService toolInfoService;
    private final ToolChannelRepoBindService bindService;
    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "启用中问答人物列表（含 admin 全局库与本渠道补充库）")
    @SaCheckPermission("channel:tool:aichat:view")
    @GetMapping("/personas")
    public R<List<ToolChannelPersonaVO>> personas() {
        return R.ok(toolInfoService.listChannelPersonas(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "可补充知识库（本渠道 + 后代渠道名下，不含平台库）")
    @SaCheckPermission("channel:tool:aichat:view")
    @GetMapping("/repos/options")
    public R<List<SystemKnowledgeRepoVO>> repoOptions() {
        return R.ok(knowledgeRepoService.listChannelScopeRepos(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "保存人物补充知识库（全量替换；空数组 = 清空补充）")
    @SaCheckPermission("channel:tool:aichat:update")
    @PutMapping("/personas/{toolCode}/repos")
    public R<Void> saveRepos(@PathVariable String toolCode,
                             @RequestBody @Valid ToolChannelRepoBindDTO dto) {
        // 人物存在性校验（不存在/非 aichat 类型抛 NOT_FOUND）
        toolInfoService.getQaPersona(toolCode);
        bindService.saveChannelRepos(toolCode, ContextHolder.getChannelCode(), dto.getRepoIds());
        return R.ok();
    }
}
```

- [ ] **步骤 6：编译验证**

```bash
cd dayan-server && mvn -q -pl dayan-modules/dayan-module-tool -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **步骤 7：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/dto/ToolChannelRepoBindDTO.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/vo/ToolChannelPersonaVO.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/controller/channel/ChannelToolAichatController.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/ToolInfoService.java dayan-server/dayan-modules/dayan-module-tool/src/main/java/com/dayan/tool/service/impl/ToolInfoServiceImpl.java
git commit -m "feat(tool): 渠道端问答人物接口——人物列表/可选库/保存补充"
```

---

### 任务 6：Starter 依赖——dayan-channel 引入 tool 模块

**文件：**
- 修改：`dayan-server/dayan-starters/dayan-channel/pom.xml`

- [ ] **步骤 1：追加模块依赖**

`dayan-starters/dayan-channel/pom.xml` 的 `<dependencies>` 中 `dayan-module-lead` 之后追加（dayan-channel starter 当前不含 tool 模块，渠道控制器无法被扫描到）：

```xml
        <dependency>
            <groupId>com.dayan</groupId>
            <artifactId>dayan-module-tool</artifactId>
        </dependency>
```

- [ ] **步骤 2：编译验证（整个渠道 starter）**

```bash
cd dayan-server && mvn -q -pl dayan-starters/dayan-channel -am compile -DskipTests
```

预期：BUILD SUCCESS，且编译输出包含 `com.dayan.tool.controller.channel.ChannelToolAichatController` 的 class。

- [ ] **步骤 3：Commit**

```bash
git add dayan-server/dayan-starters/dayan-channel/pom.xml
git commit -m "chore(channel): dayan-channel starter 引入 dayan-module-tool 依赖"
```

---

### 任务 7：渠道前端——"问答人物"页面

**文件：**
- 创建：`dayan-channel/src/types/toolAichat.ts`
- 创建：`dayan-channel/src/api/toolAichat.ts`
- 创建：`dayan-channel/src/views/tool/aichat/index.vue`

（页面路由由后端菜单驱动：`import.meta.glob('../views/**/*.vue')` 自动解析 `tool/aichat/index` 组件，无需改 router。）

- [ ] **步骤 1：创建类型**

`dayan-channel/src/types/toolAichat.ts`：

```ts
/** 渠道端问答人物（对齐后端 ToolChannelPersonaVO） */
export interface ToolChannelPersona {
  /** 工具实例编码（TL 前缀） */
  toolCode: string
  /** 人物名称 */
  personaName: string
  /** 工具简介 */
  toolDesc?: string
  /** admin 全局绑定的知识库 ID（只读） */
  globalRepoIds: number[]
  /** 本渠道补充的知识库 ID（可编辑） */
  channelRepoIds: number[]
}

/** 可补充知识库选项（对齐后端 SystemKnowledgeRepoVO 子集） */
export interface ToolChannelRepoOption {
  id: number
  repoName: string
  channelName?: string
  channelShortName?: string
}
```

- [ ] **步骤 2：创建 API**

`dayan-channel/src/api/toolAichat.ts`（对齐 `src/api/knowledge.ts` 的 `request` 用法）：

```ts
/**
 * 问答人物 API（Channel 端渠道补充知识库）。
 *
 * 对齐后端 ChannelToolAichatController（/channel-api/tools/aichat）。
 * 本渠道由后端 ContextHolder 强制注入，前端不传 channelCode。
 */
import { request } from '@/utils/request'
import type { ToolChannelPersona, ToolChannelRepoOption } from '@/types/toolAichat'

/** 启用中问答人物列表（含 admin 全局库与本渠道补充库） */
export function getChannelAichatPersonas(): Promise<ToolChannelPersona[]> {
  return request<ToolChannelPersona[]>({ url: '/channel-api/tools/aichat/personas', method: 'get' })
}

/** 可补充知识库（本渠道 + 后代渠道名下，不含平台库） */
export function getChannelAichatRepoOptions(): Promise<ToolChannelRepoOption[]> {
  return request<ToolChannelRepoOption[]>({ url: '/channel-api/tools/aichat/repos/options', method: 'get' })
}

/** 保存人物补充知识库（全量替换；空数组 = 清空补充） */
export function saveChannelPersonaRepos(toolCode: string, repoIds: number[]): Promise<void> {
  return request<void>({ url: `/channel-api/tools/aichat/personas/${toolCode}/repos`, method: 'put', data: { repoIds } })
}
```

- [ ] **步骤 3：创建页面**

`dayan-channel/src/views/tool/aichat/index.vue`（Element Plus 表格 + 编辑弹窗；全局库只读标签、补充库多选；风格对齐 `views/system/knowledge/index.vue`）：

```vue
<script setup lang="ts">
/**
 * Channel 端问答人物落地页（系统管理 → 问答人物）。
 *
 * 你问我答人物知识库两层模型：
 * - admin 全局库（config_json.repoIds）：只读展示，不可改；
 * - 本渠道补充库（tool_channel_repo_bind）：多选编辑，并集生效；
 *   可选项 = 本渠道 + 后代渠道名下的渠道库（不含平台库）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getChannelAichatPersonas,
  getChannelAichatRepoOptions,
  saveChannelPersonaRepos
} from '@/api/toolAichat'
import type { ToolChannelPersona, ToolChannelRepoOption } from '@/types/toolAichat'

const loading = ref(false)
const personas = ref<ToolChannelPersona[]>([])
const repoOptions = ref<ToolChannelRepoOption[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<{ toolCode: string; personaName: string; channelRepoIds: number[] }>({
  toolCode: '',
  personaName: '',
  channelRepoIds: []
})
const rules: FormRules = {}

/** repoId → 名称映射（全局库/补充库标签展示用） */
const repoNameMap = computed(() => new Map(repoOptions.value.map((r) => [r.id, r.repoName])))

async function loadData() {
  loading.value = true
  try {
    const [p, r] = await Promise.all([getChannelAichatPersonas(), getChannelAichatRepoOptions()])
    personas.value = p
    repoOptions.value = r
  } catch {
    ElMessage.error('加载问答人物失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function openEdit(row: ToolChannelPersona) {
  form.toolCode = row.toolCode
  form.personaName = row.personaName
  form.channelRepoIds = [...row.channelRepoIds]
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await saveChannelPersonaRepos(form.toolCode, form.channelRepoIds)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>问答人物</span>
          <span class="card-tip">渠道可为人物的知识库补充本渠道（含后代渠道）的库，与 admin 全局库并集生效</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="personas" stripe>
        <el-table-column prop="personaName" label="人物名称" min-width="140" />
        <el-table-column prop="toolDesc" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column label="admin 全局知识库（只读）" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="id in row.globalRepoIds"
              :key="id"
              size="small"
              class="repo-tag"
            >{{ repoNameMap.get(id) || `知识库#${id}` }}</el-tag>
            <span v-if="!row.globalRepoIds.length" class="empty-text">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="本渠道补充知识库" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="id in row.channelRepoIds"
              :key="id"
              type="success"
              size="small"
              class="repo-tag"
            >{{ repoNameMap.get(id) || `知识库#${id}` }}</el-tag>
            <span v-if="!row.channelRepoIds.length" class="empty-text">未补充（仅用全局库）</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑补充</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="`编辑补充知识库 · ${form.personaName}`" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="补充知识库">
          <el-select v-model="form.channelRepoIds" multiple filterable collapse-tags style="width: 100%">
            <el-option v-for="r in repoOptions" :key="r.id" :label="r.repoName" :value="r.id" />
          </el-select>
          <div class="form-tip">可选项为本渠道及后代渠道名下的知识库；留空 = 不补充（仅用 admin 全局库）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.card-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.repo-tag {
  margin: 2px 4px 2px 0;
}
.empty-text {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
</style>
```

- [ ] **步骤 4：前端构建验证**

```bash
cd dayan-channel && npm run build
```

预期：`vue-tsc --noEmit` 无类型错误，vite build 成功产出 dist。

- [ ] **步骤 5：Commit**

```bash
git add dayan-channel/src/types/toolAichat.ts dayan-channel/src/api/toolAichat.ts dayan-channel/src/views/tool/aichat/index.vue
git commit -m "feat(channel): 问答人物页——渠道补充知识库编辑（全局库只读 + 补充库多选）"
```

---

### 任务 8：整体验证

**文件：** 无新增。

- [ ] **步骤 1：全链路编译**

```bash
cd dayan-server && mvn -q -pl dayan-starters/dayan-agent,dayan-starters/dayan-channel -am compile -DskipTests
```

预期：BUILD SUCCESS（agent 与 channel 两个 starter 均通过）。

- [ ] **步骤 2：重启 channel 服务并验证接口**

重启 dayan-channel 服务（按项目日常启动方式），登录渠道控制台账号后验证：

```bash
# 人物列表（替换为真实 token）
curl -s -H "Authorization: Bearer <token>" http://localhost:8081/channel-api/tools/aichat/personas
# 可补充库（应只含本渠道与后代渠道的库）
curl -s -H "Authorization: Bearer <token>" http://localhost:8081/channel-api/tools/aichat/repos/options
# 保存补充（全量替换）
curl -s -X PUT -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"repoIds":[<本渠道库id>]}' \
  http://localhost:8081/channel-api/tools/aichat/personas/<toolCode>/repos
```

预期：列表字段含 `globalRepoIds`/`channelRepoIds`；保存后 DB `SELECT * FROM tool_channel_repo_bind;` 出现对应行；传范围外 repoId 返回 400。

- [ ] **步骤 3：agent 端人工验证（渠道差异化）**

1. 用渠道 A 账号登录 agent，进入"你问我答"选择已补充知识库的人物提问，回答应引用渠道 A 的补充库内容；
2. 用渠道 B（未补充）登录同一人物提问，回答只引用 admin 全局库内容（且全局平台库在渠道端可用——验证 `requireRepoVisibleForPersona` 放行生效）；
3. 检查 agent 的 `/agent-api/tools/aichat/configs` 返回的 `repoIds` 为各自渠道合并结果。

- [ ] **步骤 4：前端页面人工验证**

渠道控制台 → 系统管理 → 问答人物：表格展示全局库（灰标签）与补充库（绿标签）；编辑弹窗保存后表格与聊天均生效；补充库下拉不含平台库。

---

## 自检记录

- **规格覆盖度**：§2 数据模型 → 任务 1；§3.1 绑定读写 → 任务 3；§3.2 运行时合并 → 任务 4；§3.3 可见性放行 → 任务 2；§3.4 agent 列表 → 任务 4；§4.1 渠道接口 → 任务 5；§4.2 种子数据 → 任务 1；§4.3 前端页面 → 任务 7；§5 agent 零改动 → 任务 8 验证；§6 边界（范围外拒绝/残留行跳过/无渠道上下文退化）→ 任务 3（范围校验）、任务 4（merge 退化）、任务 8 步骤 3；§7 测试 → 任务 8。
- **占位符扫描**：无 TODO/待定；每个代码步骤均含完整代码与验证命令。
- **类型一致性**：`mergeRepoIds(toolCode, globalRepoIds)`、`listRepoIds(toolCode, channelCode)`、`saveChannelRepos(toolCode, channelCode, repoIds)`、`requireRepoVisibleForPersona(Long)`、`listChannelScopeRepos(String)` 在各任务中签名一致；`ToolChannelPersonaVO` 字段（toolCode/personaName/toolDesc/globalRepoIds/channelRepoIds）与前端类型对齐。
- **循环依赖规避**：`ToolChannelRepoBindServiceImpl` 不依赖 `ToolInfoService`（人物存在性校验放在控制器层），避免构造器循环依赖。
