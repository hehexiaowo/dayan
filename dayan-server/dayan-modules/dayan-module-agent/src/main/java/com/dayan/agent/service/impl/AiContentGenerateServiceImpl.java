package com.dayan.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dayan.agent.dto.AiConvertDTO;
import com.dayan.agent.dto.AiGenerateDTO;
import com.dayan.agent.dto.AiTopicsDTO;
import com.dayan.agent.service.AiContentGenerateService;
import com.dayan.agent.service.AiGenerateProgressListener;
import com.dayan.agent.service.AiMaterialAssembler;
import com.dayan.agent.vo.AiGenerateResultVO;
import com.dayan.agent.vo.AiMaterialRefsVO;
import com.dayan.agent.vo.AiMaterialSourceVO;
import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 内容生成编排实现。
 *
 * <p>素材聚合（范文全文 → 知识库 RAG → 渠道白名单商品）已抽至 {@link AiMaterialAssembler}，
 * 本类负责拼 prompt、调模型、解析输出与自检重写。全部素材分节进 prompt，
 * 仅允许基于素材写作（防幻觉）；输出按【标题】/【摘要】/【正文】标记解析，解析失败兜底整段为正文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentGenerateServiceImpl implements AiContentGenerateService {

    /** 形态说明（进 prompt） */
    private static final Map<Integer, String> FORM_INSTRUCTIONS = Map.of(
            1, "图文文章：输出 HTML 片段（可用 <h2> 小标题、<p> 段落、<ul><li> 列表），不要 <html>/<body> 外层，不要代码块围栏；篇幅 600-1200 字，首段为引入，结尾可带推荐商品与行动引导",
            2, "朋友圈文案：200 字以内口语化短文案，可带 1-2 个 emoji 与 1 个话题标签，适合直接复制转发",
            3, "视频脚本：60-90 秒口播，按「【画面】…\n【口播】…\n【字幕】…」分镜组织，3-4 个分镜");

    /** 风格档位说明（进 prompt） */
    private static final Map<String, String> STYLE_INSTRUCTIONS = Map.of(
            "professional", "专业科普风格：用词严谨、逻辑清晰、多用数据与术语，面向对养老品质有要求的家庭决策者",
            "warm", "温情软文风格：以长辈/家庭的真实生活场景切入，情感细腻、语气温暖，引发共鸣",
            "authoritative", "权威报告风格：结论先行、分点论述、数据化表达，塑造平台专业可信形象",
            "colloquial", "口语化风格：短句、亲切、像朋友聊天，适合朋友圈阅读");

    /** 目标读者指令（进 prompt；audience 归一后取值） */
    private static final Map<String, String> AUDIENCE_INSTRUCTIONS = Map.of(
            "children", "目标读者：为父母养老做决策的子女（30-50 岁）。用理性、数据与家庭责任视角，语气专业可信赖，突出“替父母安排妥当”的安心感",
            "elder", "目标读者：老人本人（55-75 岁）。直白温暖、多用短句，从老人自身利益出发（住得舒心、有人照应、不拖累子女），避免专业术语",
            "general", "目标读者：40-70 岁客户及其子女，通俗易懂");

    /** 生成后自检禁语清单（与 SYSTEM_PROMPT 合规红线一致） */
    private static final List<String> BANNED_PHRASES = List.of(
            "保证收益", "稳赚", "包赚", "最高级", "国家级", "顶级", "100%", "百分百", "绝对", "秒杀", "史上");

    /** 创作采样温度（重新生成有差异性；知识问答仍 0.3 不受影响） */
    private static final double CREATIVE_TEMPERATURE = 0.6;

    /** 转换温度（形态转换要求忠实，低于创作温度） */
    private static final double CONVERT_TEMPERATURE = 0.4;

    /** 选题温度（求多样性，高于创作温度） */
    private static final double TOPIC_TEMPERATURE = 0.8;

    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigGoodsService channelConfigGoodsService;
    private final AiMaterialAssembler materialAssembler;
    private final SystemConfigService systemConfigService;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    @Override
    public AiGenerateResultVO generate(AiGenerateDTO dto) {
        return generate(dto, null);
    }

    @Override
    public AiGenerateResultVO generate(AiGenerateDTO dto, AiGenerateProgressListener listener) {
        if (dto.getContentType() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容形态必选");
        }
        String formInstruction = FORM_INSTRUCTIONS.get(dto.getContentType());
        if (formInstruction == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容形态取值 1-3");
        }
        String styleInstruction = STYLE_INSTRUCTIONS.get(dto.getStyleCode());
        if (styleInstruction == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的写作风格: " + dto.getStyleCode());
        }
        String channelCode = ContextHolder.getChannelCode();
        long startMillis = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        notifyStage(listener, "material", "正在准备素材…");

        // ---------- 1. 素材聚合（范文/RAG/商品，逻辑见 AiMaterialAssembler） ----------
        AiMaterialRefsVO refs = new AiMaterialRefsVO();
        refs.setRefContentCode(dto.getRefContentCode());
        refs.setKbFileIds(dto.getKbFileIds());
        refs.setGoodsCodes(dto.getGoodsCodes());
        AiMaterialAssembler.MaterialBundle bundle =
                materialAssembler.assemble(channelCode, refs, dto.getTopic(), warnings);
        String material = bundle.blocks();
        List<AiMaterialSourceVO> sources = bundle.sources();
        int materialCount = bundle.blockCount();
        // 商品名收集（自检用）
        List<String> selectedGoodsNames = new ArrayList<>();
        if (dto.getGoodsCodes() != null) {
            for (String goodsCode : dto.getGoodsCodes()) {
                selectedGoodsNames.add(goodsInfoService.getDetail(goodsCode).getGoodsName());
            }
        }

        // ---------- 2. 拼 prompt 并调用 ----------
        String userPrompt = buildUserPrompt(dto, formInstruction, styleInstruction, material);
        String apiKey = requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员");
        String apiHost = requireConfig("llm.api-host", "AI 网关未配置，请联系管理员");
        String model = StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus");
        notifyStage(listener, "composing", "正在撰写内容…");
        String answer = callChat(apiKey, apiHost, model, userPrompt, CREATIVE_TEMPERATURE, listener);

        // ---------- 3. 解析输出 + 自检（失败自动重写一次，仍失败降级 warning） ----------
        AiGenerateResultVO result = parseAnswer(dto.getContentType(), answer, dto.getTopic());
        List<String> errors = selfCheck(result, dto, selectedGoodsNames);
        if (!errors.isEmpty()) {
            notifyStage(listener, "rewriting", "自检未通过，正在优化重写…");
            if (listener != null) {
                listener.onReset();
            }
            String rewritePrompt = buildRewritePrompt(userPrompt, answer, errors);
            answer = callChat(apiKey, apiHost, model, rewritePrompt, CREATIVE_TEMPERATURE, listener);
            result = parseAnswer(dto.getContentType(), answer, dto.getTopic());
            warnings.addAll(selfCheck(result, dto, selectedGoodsNames));
        }
        result.setWarnings(warnings);
        result.setSources(sources);
        log.info("AI 生成完成 channel={} contentType={} style={} refContent={} kbFiles={} goods={} materialBlocks={} sources={} costMs={}",
                channelCode, dto.getContentType(), dto.getStyleCode(), dto.getRefContentCode(),
                dto.getKbFileIds() == null ? 0 : dto.getKbFileIds().size(),
                dto.getGoodsCodes() == null ? 0 : dto.getGoodsCodes().size(),
                materialCount, sources.size(), System.currentTimeMillis() - startMillis);
        return result;
    }

    /** 统一 chat 调用（listener 非空走流式，增量回调） */
    private String callChat(String apiKey, String apiHost, String model, String prompt,
                            double temperature, AiGenerateProgressListener listener) {
        return listener == null
                ? bailianChatClient.chat(apiKey, apiHost, model, SYSTEM_PROMPT, prompt, temperature)
                : bailianChatClient.chatStream(apiKey, apiHost, model, SYSTEM_PROMPT, prompt,
                        temperature, listener::onDelta);
    }

    /** 自动重写 prompt：原任务 + 初稿全文 + 修正清单 */
    private String buildRewritePrompt(String originalPrompt, String draft, List<String> errors) {
        return originalPrompt + "\n\n【上一版初稿】\n" + draft
                + "\n\n【修正要求】\n上一版存在以下问题，请修正后按【输出格式】完整重新输出：\n- "
                + String.join("\n- ", errors);
    }

    @Override
    public List<String> suggestTopics(AiTopicsDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        // 素材：勾选文档名 + 商品名与卖点（轻量，不做 RAG 全文）
        StringBuilder material = new StringBuilder();
        List<String> docNames = materialAssembler.resolveKbFileNames(channelCode, dto == null ? null : dto.getKbFileIds());
        if (!docNames.isEmpty()) {
            material.append("【知识库文档】").append(String.join("、", docNames)).append('\n');
        }
        List<String> goodsCodes = dto == null ? null : dto.getGoodsCodes();
        if (goodsCodes != null && !goodsCodes.isEmpty()) {
            Set<String> whitelist = channelConfigGoodsService.listByChannel(channelCode).stream()
                    .map(ChannelConfigGoods::getGoodsCode).collect(Collectors.toSet());
            material.append("【商品】\n");
            for (String goodsCode : goodsCodes) {
                if (!whitelist.contains(goodsCode)) {
                    throw new BusinessException(ErrorCode.BUSINESS, "商品不在可购范围: " + goodsCode);
                }
                GoodsInfoVO g = goodsInfoService.getDetail(goodsCode);
                material.append("- ").append(g.getGoodsName())
                        .append(g.getSummary() == null ? "" : "：" + g.getSummary()).append('\n');
            }
        }
        if (material.isEmpty()) {
            material.append("（未指定素材，可围绕养老规划、旅居权益、家庭关怀等通用方向）");
        }
        String prompt = """
                你是「大雁养老」的营销选题顾问，帮保险/养老代理人想公众号/朋友圈的获客选题。
                当前时节：%s。
                【可用素材】
                %s
                请给出 5 个选题，每行一个，格式为"序号、选题一句话（15-30 字）"。
                要求：贴合素材与时节、突出具体利益点或客户痛点、口语化不空泛、不使用绝对化与收益承诺用语、不得使用“免费”等与商品实际价格不符的表述。""".formatted(seasonHint(), material);
        String answer = callChat(
                requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                prompt, TOPIC_TEMPERATURE, null);
        return parseTopics(answer);
    }

    @Override
    public AiGenerateResultVO convert(AiConvertDTO dto) {
        String formInstruction = FORM_INSTRUCTIONS.get(dto.getTargetContentType());
        if (formInstruction == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标形态取值 1-3");
        }
        String styleInstruction = dto.getStyleCode() == null
                ? "沿用原稿的既有风格与语气" : STYLE_INSTRUCTIONS.getOrDefault(dto.getStyleCode(), "沿用原稿的既有风格与语气");
        StringBuilder sb = new StringBuilder();
        sb.append("【改写任务】\n把下面这篇内容改写为另一种发布形态，核心事实（权益档位、价格、商品名、机构信息）必须与原稿完全一致，不得新增或修改任何事实，不得出现绝对化与收益承诺用语。\n");
        sb.append("目标形态：").append(formInstruction).append('\n');
        sb.append("风格：").append(styleInstruction).append('\n');
        sb.append("\n【输出格式】\n严格按以下标记输出，不要输出其他内容：\n【标题】xxx\n【摘要】xxx\n【正文】xxx\n【备选标题】另拟 3 个风格不同的备选标题，用分号分隔\n\n");
        sb.append("【原稿】\n标题：").append(dto.getTitle()).append('\n');
        if (StrUtil.isNotBlank(dto.getSummary())) {
            sb.append("摘要：").append(dto.getSummary()).append('\n');
        }
        sb.append("正文：\n").append(stripHtml(dto.getContentBody()));
        String answer = callChat(
                requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                sb.toString(), CONVERT_TEMPERATURE, null);
        return parseAnswer(dto.getTargetContentType(), answer, dto.getTitle());
    }

    /** 按月给出季节/节日提示（选题贴合时节） */
    private String seasonHint() {
        return switch (LocalDate.now().getMonthValue()) {
            case 12, 1, 2 -> "冬季（跨年/春节/寒假，旅居过冬、防寒养生、团圆话题）";
            case 3, 4, 5 -> "春季（踏青/清明/五一/母亲节，出行、健康体检、感恩话题）";
            case 6, 7, 8 -> "夏季（端午/父亲节/暑假，避暑旅居、亲子陪伴话题）";
            default -> "秋季（中秋/国庆/重阳节，敬老、秋游、团聚话题）";
        };
    }

    /** 解析选题输出：按行拆、去编号前缀、取前 5 */
    private List<String> parseTopics(String answer) {
        return Arrays.stream(answer.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("（") && !s.startsWith("好的"))
                .map(s -> s.replaceFirst("^\\d+\\s*[、.．)）]\\s*", ""))
                .filter(s -> s.length() >= 8 && s.length() <= 60)
                .limit(5)
                .toList();
    }

    /** 阶段回调（listener 为空时忽略，非流式零开销） */
    private void notifyStage(AiGenerateProgressListener listener, String stage, String message) {
        if (listener != null) {
            listener.onStage(stage, message);
        }
    }

    /** 系统提示词：防幻觉铁律 + 合规红线 */
    private static final String SYSTEM_PROMPT = """
            你是「大雁养老」的资深内容创作助手，帮助保险/养老代理人撰写获客营销内容。
            铁律：
            1. 所有事实（权益档位、价格、机构名、数据、产品信息）只能来自用户消息的【素材】部分，素材没有的信息一律不得编造；
            2. 不虚构任何机构名称、价格、日期、政策；
            3. 语言自然流畅，避免空泛套话（如“综上所述”“赋能”“值得一提的是”），避免堆砌形容词；
            4. 全文简体中文；
            5. 合规红线：不得出现“保证收益、稳赚、包赚、最高级、最好、第一、国家级、顶级、100%、百分百、绝对、秒杀、史上”等绝对化或收益承诺用语；不得提及具体保险产品名称与费率。""";

    private String buildUserPrompt(AiGenerateDTO dto, String formInstruction, String styleInstruction, String material) {
        String audienceKey = StrUtil.blankToDefault(dto.getAudience(), "general");
        String audienceInstruction = AUDIENCE_INSTRUCTIONS.getOrDefault(audienceKey,
                AUDIENCE_INSTRUCTIONS.get("general"));
        StringBuilder sb = new StringBuilder();
        sb.append("【写作任务】\n");
        sb.append("形态：").append(formInstruction).append('\n');
        sb.append("风格：").append(styleInstruction).append('\n');
        sb.append("读者：").append(audienceInstruction).append('\n');
        if (StrUtil.isNotBlank(dto.getRefContentCode())) {
            sb.append("范文仿写：请模仿【素材】中参考范文的语气与行文结构，事实一律以素材为准。\n");
        }
        sb.append("主题：").append(StrUtil.isNotBlank(dto.getTopic()) ? dto.getTopic() : "（缺省，请从素材归纳一个具体主题）").append('\n');
        sb.append("\n【输出格式】\n严格按以下标记输出，不要输出其他内容：\n【标题】xxx\n【摘要】xxx\n【正文】xxx\n【备选标题】另拟 3 个风格不同的备选标题，用分号分隔\n\n");
        sb.append("【素材】\n").append(StrUtil.isBlank(material) ? "（无素材，请基于养老行业常识谨慎写作，并避免具体数字）" : material);
        return sb.toString();
    }

    /** 解析模型输出：按标记拆 标题/摘要/正文/备选标题，缺标题时按形态兜底 */
    private AiGenerateResultVO parseAnswer(int contentType, String answer, String topic) {
        AiGenerateResultVO vo = new AiGenerateResultVO();
        vo.setContentType(contentType);
        String title = extract(answer, "【标题】");
        String summary = extract(answer, "【摘要】");
        String body = extract(answer, "【正文】");
        if (StrUtil.isBlank(body)) {
            // 解析失败兜底：整段作为正文（清理标记残留）
            body = answer.replaceAll("【标题】.*?\\n", "")
                    .replaceAll("【摘要】.*?\\n", "")
                    .replaceAll("【备选标题】.*?(\\n|$)", "")
                    .trim();
        }
        if (StrUtil.isBlank(title)) {
            if (contentType == 2 && StrUtil.isNotBlank(body)) {
                title = StrUtil.maxLength(body.trim(), 20); // 朋友圈：正文首句兜底
            } else {
                title = StrUtil.isNotBlank(topic) ? topic : "AI 生成内容";
            }
        }
        vo.setTitle(title);
        vo.setSummary(summary);
        vo.setContentBody(body);
        String alt = extract(answer, "【备选标题】");
        if (StrUtil.isNotBlank(alt)) {
            final String mainTitle = title;
            vo.setAlternativeTitles(Arrays.stream(alt.split("[；;\\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.equals(mainTitle))
                    .limit(3)
                    .toList());
        }
        return vo;
    }

    /** 输出标记（extract 截断边界用） */
    private static final List<String> OUTPUT_MARKERS = List.of("【标题】", "【摘要】", "【正文】", "【备选标题】");

    /** 提取【标记】后的内容（到下一已知标记或结尾；不识别正文内的业务标记如【画面】） */
    private String extract(String answer, String marker) {
        int start = answer.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int contentStart = start + marker.length();
        int end = answer.length();
        for (String m : OUTPUT_MARKERS) {
            if (m.equals(marker)) {
                continue;
            }
            int idx = answer.indexOf(m, contentStart);
            if (idx >= 0 && idx < end) {
                end = idx;
            }
        }
        return answer.substring(contentStart, end).trim();
    }

    /** 生成后规则自检（不调 LLM）：篇幅/商品融入/禁语 → 返回可修正错误（驱动自动重写） */
    private List<String> selfCheck(AiGenerateResultVO result, AiGenerateDTO dto, List<String> goodsNames) {
        List<String> errors = new ArrayList<>();
        String plain = stripHtml(result.getContentBody());
        if (dto.getContentType() != null && dto.getContentType() == 1) {
            int len = plain.replaceAll("\\s", "").length();
            if (len < 300) {
                errors.add("正文篇幅不足（约 " + len + " 字），请扩写至 600 字以上");
            } else if (len > 3000) {
                errors.add("正文超长（约 " + len + " 字），请精简至 1200 字以内");
            }
        }
        if (goodsNames != null && !goodsNames.isEmpty()) {
            boolean mentioned = goodsNames.stream().anyMatch(n -> n != null && result.getContentBody().contains(n));
            if (!mentioned) {
                errors.add("勾选的推荐商品（" + String.join("、", goodsNames) + "）未融入正文，请自然融入其名称与卖点");
            }
        }
        for (String banned : BANNED_PHRASES) {
            if (plain.contains(banned)) {
                errors.add("正文含绝对化用语「" + banned + "」，请改写为合规表达并全文复查类似用语");
                break;
            }
        }
        return errors;
    }

    /** HTML 去标签（正文素材用，保留纯文本） */
    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return StrUtil.maxLength(text, AiMaterialAssembler.REF_CONTENT_MAX);
    }

    private String requireConfig(String key, String message) {
        String value = getConfig(key);
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(ErrorCode.BUSINESS, message);
        }
        return value;
    }

    private String getConfig(String key) {
        return systemConfigService.getValue("llm", key);
    }
}
