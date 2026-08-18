# AI 问答最终审查修复报告

## 修复项

1. 权限种子：在 `db/migration/seed/rbac_resource_perm.sql` 的 tool 域权限段新增 `tool:qa:list/query/create/update/delete` 五项 `organ_permission`，复用既有 `admin_resource_tool` 菜单，排序为 405-409。
2. 多轮上下文：`ToolAiQaChatServiceImpl` 在非流式请求中使用 `BailianChatClient` 多消息重载，按消息 ID 升序携带本会话最近 12 条 user/assistant 历史，每条最多 4000 字符；流式请求将同一历史拼入 user prompt。知识资料仍保留在 system prompt，当前问题只作为最终 user 消息，不写入历史。
3. 会话标题：首轮用户消息入库后，以 Unicode code point 截断至 30 字符更新标题；通过 `message_count = 0` 条件更新避免覆盖后续标题，同时保留消息数原子增量。
4. Agent 对话：临时消息 ID 改为正数递增，`scroll-into-view` 锚点对应实际 DOM ID；移除未使用 loading 状态；新建会话前显示确认弹窗。
5. sessions 参数：后端对空值和非正 `configId` 抛出 `BusinessException(ErrorCode.PARAM_ERROR, ...)`；前端无效 ID 直接返回空数组，不发请求。
6. Agent 类型：`QaSession.title` 改为必填 string。

## 测试

- `cd F:\code\dayan\dayan-server && mvn -q -o -pl dayan-modules/dayan-module-tool -am compile`：通过。
- `cd F:\code\dayan\dayan-admin && node ./node_modules/vue-tsc/bin/vue-tsc.js --noEmit`：通过。
- `cd F:\code\dayan\dayan-agent && node ./node_modules/vue-tsc/bin/vue-tsc.js --noEmit`：失败，既有无关错误：
  - `src/pages/business/park/vital/district.vue:111`，`Promise<Map | null>` 不能赋给 `Map`。
  - `src/pages/login/index.vue:330`，`ImportMeta` 不存在 `env` 属性。
- `git diff --check`：通过。

## 提交

`5333df8800990082c5e77b9d57e276275f2a0c39`。
