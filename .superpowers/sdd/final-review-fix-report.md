# 知识仓库最终审查修复报告（Important × 3 + Minor × 3）

## 修复项

1. **文档列表元数据富化（Important）**：`SystemKnowledgeRepoServiceImpl.listDocuments` 对每个文档调用
   `describeFile(fileId)` 逐行补全 `categoryId/tags/parser`（ListIndexDocuments 不返回这三项），单行失败
   try-catch 容错保持 null 并 warn 日志；`parseStatus` 仅在列表数据缺失时用 DescribeFile 回填。
   前端"详情"弹窗（标签列、"所属类目"）数据源由此补全。
2. **OverlapSize 在 regex 切分模式无效（Important，规格层）**：百炼 API 重叠仅在 length 切分模式生效，
   本项目自定义切分为 regex 模式。admin/channel 双端创建弹窗 regex 子区移除"重叠长度"输入项（分隔符/
   切块长度保留），表单提示文案同步去掉"重叠"；详情页保留重叠长度展示（兼容旧数据）；
   规格文档 §4.1 overlapSize 字段说明与 §4.6 创建弹窗描述同步修订。
3. **channel 缺已建库检索参数编辑（Important，计划遗漏）**：`dayan-channel/.../knowledge/index.vue`
   仓库摘要卡新增"检索参数"编辑区（仅本渠道且 indexId 非空时显示）：denseTopK/sparseTopK/rerankMinScore
   行内 el-input-number + 保存按钮，保存时提交完整 indexConfig（原值 + 三项修改），对齐 admin 详情页实现；
   切换树节点/刷新树时同步表单。
4. **客户端注释澄清（Minor）**：`deleteCategory` 调用点注释说明 SDK 签名 (CategoryId, WorkspaceId) 与路径
   模板 `/{ws}/datacenter/category/{cateId}/` 的对应关系（传参 (categoryId, workspaceId) 正确）；
   `describeFile` 空 `DescribeFileRequest` 加一行注释。
5. **listCategories 翻页上限（Minor）**：while(true) 改为 for 100 轮上限（每轮 100 条 ≈ 10000 条），
   超限抛 `BusinessException` 防服务端 nextToken 异常死循环。
6. **MessageBox 取消兜底（Minor）**：admin/channel 双端 `KnowledgeCategoryDialog` 的 handleAdd/handleDelete
   改为 try/catch 包裹 ElMessageBox.prompt/confirm，取消时吞掉拒绝并提前 return（不会误执行增删）。

## 测试

- `cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system -am test
  -Dtest='SystemKnowledgeIndexConfigTest,SystemKnowledgeRepoServiceImplTest'
  -Dsurefire.failIfNoSpecifiedTests=false`：Tests run: 9, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`：零错误。
- `cd F:\code\dayan\dayan-channel && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`：零错误。
- `git diff --check` / 工作树干净。

## 提交

- `1eda009` fix(server): 文档列表富化类目/标签/解析器元数据（DescribeFile 逐行补全）
- `6e96a31` fix(admin,channel): 自定义切分表单移除无效的重叠长度配置（百炼 regex 模式不生效）
- `88521a6` feat(channel): 已建库知识仓库检索参数编辑（对齐 admin 详情页）
- `e1cb20a` chore(server,admin,channel): 客户端注释澄清/listCategories 翻页上限/MessageBox 取消兜底

## 备注

- Critical 1 经 SDK 源码核实为误报，未处理（按任务说明）。
- channel 检索参数编辑提交完整 indexConfig（含不可变字段原值），后端 `assertUpdatableConfig` 仅比较
  非 null 字段，部分提交不会误拒。
