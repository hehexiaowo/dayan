# 任务：生成文章大纲（阶段 2）

【最高优先级：核心写作策略约束——绝不允许偏离】
{{core_execution_prompt}}

【策略面板】
- 精准受众：{{target_audience}}
- 核心痛点：{{core_pain_point}}
- 爆款逻辑：{{viral_logic}}
- 优势放大器：{{advantage_hook}}

{{purpose_rule}}

{{platform_rules}}

【选定标题】{{selected_title}}
【主题】{{topic}}

【硬数据清单（论据唯一来源）】
{{fact_digest}}

【素材原文】
{{material}}

【论据铁律】每个节点的 arguments 必须写明引用素材里的具体数据/事实（如"引用知识库中居家护理等待期 60 天"），严禁写"此处放一个案例"这种空指令。

【严禁脱题】主题必须写入开头节点作为钩子；绝不能抛下主题只写产品资料。

【AI 配图 prompt 视觉净化铁律】
1. prompt 纯英文 ≤60 词，以 "Warm lifestyle photograph" / "Bright lifestyle photograph" / "Muted editorial photograph" 开头。
2. 真实相机可拍的单一场景、1 个主体；含摄影术语（shallow depth of field, 85mm f/1.8 lens）。
3. 禁止：抽象拼贴（硬币堆山/天平/雨伞叠加）、商业拼图（logo/合同特写/计算器+钱币）、阴暗负面（dark/moody/gloomy）、chaos/storm/swirl/scattered/surreal/abstract/metaphorical。
4. 尺寸只能：1024*1024 / 1280*720 / 1080*1440。

【输出】严格 JSON（camelCase）：
{
  "coverImage": {"size": "封面尺寸", "prompt": "英文摄影描述", "imagePromptZh": "中文场景描述"},
  "nodes": [
    {"id": "node_1", "section": "小节标题（开头/主体1/结尾）",
     "corePoints": ["要点1", "要点2"],
     "arguments": ["引用素材的具体数据/事实"],
     "viralTags": ["🪝 开头钩子"],
     "imageInsertion": null}
  ]
}
配图位规划：{{image_count_hint}}；不需要配图的节点 imageInsertion 为 null。
