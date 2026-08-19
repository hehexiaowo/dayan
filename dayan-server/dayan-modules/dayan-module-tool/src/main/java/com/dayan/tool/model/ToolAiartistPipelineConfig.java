package com.dayan.tool.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 创作分类流水线配置（tool_info.config_json.pipeline 解析结果）。
 *
 * <p>六阶段流水线的全部可调参数：各阶段温度、合规禁语、素材上限、标题/篇幅限制、
 * 配图规格与超时、形态/风格/受众文案、阶段提示词覆盖。缺省回落内置默认值，
 * 保证旧分类（config_json 无 pipeline）行为与硬编码时期完全一致。
 */
@Data
public class ToolAiartistPipelineConfig {

    /** 配图降级 prompt 默认模板（{promptZh} 为中文场景描述占位） */
    public static final String DEFAULT_IMAGE_FALLBACK_PROMPT =
            "Warm lifestyle photograph, elderly care concept related to: {promptZh}, single subject, shallow depth of field";

    /** 默认正文篇幅窗口（形态缺失时兜底） */
    private static final int[] DEFAULT_LENGTH_WINDOW = {800, 2500};
    /** 默认配图规划提示（形态缺失时兜底） */
    private static final String DEFAULT_IMAGE_COUNT_HINT =
            "coverImage 1 张（1024*1024）+ 正文节点配图 3-4 张（1280*720）";

    /** 分类人设（config_json 根级 systemPrompt，注入各阶段提示词开头） */
    private String systemPrompt = "";

    /** 绑定的知识库仓库（config_json 根级 repoIds；正文生成前自动检索补充用，空则不检索） */
    private List<Long> repoIds = List.of();

    /** 全局系统提示词（pipeline.system；空回落 ai-prompts/system.md） */
    private String system = "";

    /** 目的规则（pipeline.purposeRules：science/park/product；空回落 ai-prompts/purpose/*.md） */
    private Map<String, String> purposeRules = Map.of();

    /** 平台规则（pipeline.platformRules：mp/xhs/moment/script；空回落 ai-prompts/platform/*.md） */
    private Map<String, String> platformRules = Map.of();

    // ---------- 各阶段温度 ----------

    private double digestTemp = 0.2;
    private double strategyTemp = 0.7;
    private double titlesTemp = 0.7;
    private double outlineTemp = 0.5;
    private double bodyTemp = 0.6;
    private double auditTemp = 0.2;
    private double polishTemp = 0.5;
    private double reviseTemp = 0.3;

    // ---------- 合规与生成限制 ----------

    /** 生成后自检禁语清单（与 system.md 合规红线一致） */
    private List<String> bannedPhrases = List.of(
            "保证收益", "稳赚", "包赚", "最高级", "国家级", "顶级", "100%", "百分百", "绝对", "秒杀", "史上");

    /** 素材快照渲染总量上限（超出截断并记 warning） */
    private int materialMax = 8000;

    /** 标题条数上限 */
    private int titleCountLimit = 5;

    /** 标题感染力评分夹取区间 */
    private int scoreMin = 70;
    private int scoreMax = 99;

    /** 润色版保留阈值（正文长度比，低于则保留审计版） */
    private double polishKeepRatio = 0.95;

    // ---------- 配图 ----------

    /** 单张配图任务轮询上限 */
    private long imagePollTimeoutMs = 90_000L;

    /** 配图连续失败降级阈值 */
    private int imageRetryAfterFailures = 2;

    /** 配图降级 prompt 模板（{promptZh} 占位） */
    private String imageFallbackPrompt = DEFAULT_IMAGE_FALLBACK_PROMPT;

    /** 封面默认尺寸 / 小红书封面尺寸 / 节点配图尺寸 */
    private String coverSizeDefault = "1024*1024";
    private String coverSizeXhs = "1080*1440";
    private String nodeSize = "1280*720";

    // ---------- 形态/风格/受众文案 ----------

    /** 各形态标题字数上限（titles-regen 用） */
    private Map<Integer, Integer> titleLimits = new HashMap<>(Map.of(1, 30, 2, 20, 3, 15, 4, 20));

    /** 各形态正文篇幅窗口 [min, max]（ruleCheck 自检用） */
    private Map<Integer, int[]> lengthWindows = new HashMap<>(Map.of(
            1, new int[]{800, 2500},
            2, new int[]{30, 400},
            3, new int[]{400, 2500},
            4, new int[]{350, 1500}));

    /** 各形态配图规划提示（outline 阶段注入） */
    private Map<Integer, String> imageCountHints = new HashMap<>(Map.of(
            1, DEFAULT_IMAGE_COUNT_HINT,
            3, "仅规划 coverImage 1 张（1024*1024），所有 nodes 的 imageInsertion 必须为 null",
            4, "coverImage 1 张（1080*1440）+ 节点配位合计 2-4 张（1280*720）"));

    /** 各形态写作指令（strategy 阶段注入） */
    private Map<Integer, String> formInstructions = new HashMap<>(Map.of(
            1, "微信公众号精品图文（1200-1500 字，HTML 片段 <h2>/<p>，标题 ≤30 字）",
            2, "朋友圈文案（≤200 字纯文本 + 1-2 emoji + 1 个 #话题标签，标题=首句钩子 ≤20 字）",
            3, "短视频口播脚本（60-90 秒，【画面】【口播】【字幕】分镜，标题 ≤15 字）",
            4, "小红书笔记（600-800 字，Emoji 列表 + #标签段，标题 ≤20 字）"));

    /** 风格指令（strategy 阶段注入） */
    private Map<String, String> styleInstructions = new HashMap<>(Map.of(
            "professional", "专业科普风格：用词严谨、逻辑清晰、多用数据与术语，面向对养老品质有要求的家庭决策者",
            "warm", "温情软文风格：以长辈/家庭的真实生活场景切入，情感细腻、语气温暖，引发共鸣",
            "authoritative", "权威报告风格：结论先行、分点论述、数据化表达，塑造平台专业可信形象",
            "colloquial", "口语化风格：短句、亲切、像朋友聊天"));

    /** 受众指令（strategy 阶段注入） */
    private Map<String, String> audienceInstructions = new HashMap<>(Map.of(
            "children", "为父母养老做决策的子女（30-50 岁）：理性、数据与家庭责任视角，专业可信赖",
            "elder", "老人本人（55-75 岁）：直白温暖、短句、从老人自身利益出发，避免术语",
            "general", "40-70 岁客户及其子女：通俗易懂"));

    // ---------- 阶段提示词覆盖 ----------

    /** 阶段提示词覆盖（key=提示词名，如 strategy/body；空或缺失走 ai-prompts 资源） */
    private Map<String, String> prompts = new HashMap<>();

    /** 正文篇幅窗口（形态缺失时兜底） */
    public int[] lengthWindowOf(Integer contentType) {
        return lengthWindows.getOrDefault(contentType == null ? 1 : contentType, DEFAULT_LENGTH_WINDOW);
    }

    /** 标题字数上限（形态缺失时兜底 30） */
    public int titleLimitOf(Integer contentType) {
        return titleLimits.getOrDefault(contentType == null ? 1 : contentType, 30);
    }

    /** 配图规划提示（形态缺失时兜底） */
    public String imageCountHintOf(Integer contentType) {
        return imageCountHints.getOrDefault(contentType == null ? 1 : contentType, DEFAULT_IMAGE_COUNT_HINT);
    }

    /**
     * 解析 config_json（根级 systemPrompt + pipeline 对象）；pipeline 缺失或字段缺失
     * 时该字段保持默认值。传入 null 或空对象返回全默认配置。
     */
    public static ToolAiartistPipelineConfig parse(JSONObject cfg) {
        ToolAiartistPipelineConfig c = new ToolAiartistPipelineConfig();
        if (cfg == null) {
            return c;
        }
        c.setSystemPrompt(StrUtil.nullToEmpty(cfg.getStr("systemPrompt")));
        if (cfg.getJSONArray("repoIds") != null) {
            c.setRepoIds(cfg.getJSONArray("repoIds").toList(Long.class));
        }
        JSONObject pipe = cfg.getJSONObject("pipeline");
        if (pipe == null) {
            return c;
        }
        c.setSystem(StrUtil.nullToEmpty(pipe.getStr("system")));
        c.setPurposeRules(mergeStrs(pipe.getJSONObject("purposeRules"), Map.of()));
        c.setPlatformRules(mergeStrs(pipe.getJSONObject("platformRules"), Map.of()));
        JSONObject temps = pipe.getJSONObject("temps");
        if (temps != null) {
            c.setDigestTemp(temps.getDouble("digest", c.getDigestTemp()));
            c.setStrategyTemp(temps.getDouble("strategy", c.getStrategyTemp()));
            c.setTitlesTemp(temps.getDouble("titles", c.getTitlesTemp()));
            c.setOutlineTemp(temps.getDouble("outline", c.getOutlineTemp()));
            c.setBodyTemp(temps.getDouble("body", c.getBodyTemp()));
            c.setAuditTemp(temps.getDouble("audit", c.getAuditTemp()));
            c.setPolishTemp(temps.getDouble("polish", c.getPolishTemp()));
            c.setReviseTemp(temps.getDouble("revise", c.getReviseTemp()));
        }
        if (pipe.getJSONArray("bannedPhrases") != null) {
            c.setBannedPhrases(pipe.getJSONArray("bannedPhrases").toList(String.class));
        }
        c.setMaterialMax(pipe.getInt("materialMax", c.getMaterialMax()));
        c.setTitleCountLimit(pipe.getInt("titleCountLimit", c.getTitleCountLimit()));
        JSONArray scoreRange = pipe.getJSONArray("scoreRange");
        if (scoreRange != null && scoreRange.size() >= 2) {
            c.setScoreMin(scoreRange.getInt(0));
            c.setScoreMax(scoreRange.getInt(1));
        }
        c.setPolishKeepRatio(pipe.getDouble("polishKeepRatio", c.getPolishKeepRatio()));
        c.setImagePollTimeoutMs(pipe.getLong("imagePollTimeoutMs", c.getImagePollTimeoutMs()));
        c.setImageRetryAfterFailures(pipe.getInt("imageRetryAfterFailures", c.getImageRetryAfterFailures()));
        c.setImageFallbackPrompt(pipe.getStr("imageFallbackPrompt", c.getImageFallbackPrompt()));
        c.setCoverSizeDefault(pipe.getStr("coverSizeDefault", c.getCoverSizeDefault()));
        c.setCoverSizeXhs(pipe.getStr("coverSizeXhs", c.getCoverSizeXhs()));
        c.setNodeSize(pipe.getStr("nodeSize", c.getNodeSize()));
        c.setTitleLimits(mergeInts(pipe.getJSONObject("titleLimits"), c.getTitleLimits()));
        c.setLengthWindows(mergeWindows(pipe.getJSONObject("lengthWindows"), c.getLengthWindows()));
        c.setImageCountHints(mergeIntStrs(pipe.getJSONObject("imageCountHints"), c.getImageCountHints()));
        c.setFormInstructions(mergeIntStrs(pipe.getJSONObject("formInstructions"), c.getFormInstructions()));
        c.setStyleInstructions(mergeStrs(pipe.getJSONObject("styleInstructions"), c.getStyleInstructions()));
        c.setAudienceInstructions(mergeStrs(pipe.getJSONObject("audienceInstructions"), c.getAudienceInstructions()));
        c.setPrompts(mergeStrs(pipe.getJSONObject("prompts"), Map.of()));
        return c;
    }

    /** 字符串 map 合并：默认值打底，配置覆盖/新增 */
    private static Map<String, String> mergeStrs(JSONObject src, Map<String, String> defaults) {
        Map<String, String> out = new LinkedHashMap<>(defaults);
        if (src != null) {
            for (Map.Entry<String, Object> en : src.entrySet()) {
                out.put(en.getKey(), en.getValue() == null ? null : String.valueOf(en.getValue()));
            }
        }
        return out;
    }

    /** 整数 map 合并（key 为数字形态编码的字符串形式） */
    private static Map<Integer, Integer> mergeInts(JSONObject src, Map<Integer, Integer> defaults) {
        Map<Integer, Integer> out = new HashMap<>(defaults);
        if (src != null) {
            for (Map.Entry<String, Object> en : src.entrySet()) {
                Object v = en.getValue();
                out.put(Integer.parseInt(en.getKey()),
                        v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v)));
            }
        }
        return out;
    }

    /** 数字形态编码 → 文案 的 map 合并 */
    private static Map<Integer, String> mergeIntStrs(JSONObject src, Map<Integer, String> defaults) {
        Map<Integer, String> out = new HashMap<>(defaults);
        if (src != null) {
            for (Map.Entry<String, Object> en : src.entrySet()) {
                out.put(Integer.parseInt(en.getKey()),
                        en.getValue() == null ? null : String.valueOf(en.getValue()));
            }
        }
        return out;
    }

    /** 篇幅窗口合并：配置值形如 [min, max] */
    private static Map<Integer, int[]> mergeWindows(JSONObject src, Map<Integer, int[]> defaults) {
        Map<Integer, int[]> out = new HashMap<>();
        defaults.forEach((k, v) -> out.put(k, v.clone()));
        if (src != null) {
            for (Map.Entry<String, Object> en : src.entrySet()) {
                JSONArray arr = (JSONArray) en.getValue();
                out.put(Integer.parseInt(en.getKey()), new int[]{arr.getInt(0), arr.getInt(1)});
            }
        }
        return out;
    }
}
