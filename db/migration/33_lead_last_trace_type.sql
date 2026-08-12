-- migration 33: agent_lead 增加 last_trace_type（最后互动类型）
-- 与 last_trace_time / trace_count 同为去规范化字段，供线索列表卡片直接展示，
-- 避免列表查询时对 agent_lead_trace 做 N+1 关联。
-- 取值：1=浏览内容 2=使用工具 3=查看海报（与 agent_lead_trace.trace_type 一致）

ALTER TABLE agent_lead
  ADD COLUMN last_trace_type INT DEFAULT NULL COMMENT '最后互动类型（1=内容 2=工具 3=海报）';
