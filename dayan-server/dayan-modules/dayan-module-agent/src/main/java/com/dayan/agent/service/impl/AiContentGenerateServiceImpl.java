package com.dayan.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dayan.agent.dto.AiConvertDTO;
import com.dayan.agent.dto.AiTopicsDTO;
import com.dayan.agent.service.AiContentGenerateService;
import com.dayan.agent.service.AiMaterialAssembler;
import com.dayan.agent.util.AiPrompts;
import com.dayan.agent.vo.AiGenerateResultVO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 内容轻量编排实现：选题灵感 + 形态转换。
 *
 * <p>单次生成链路已由六阶段流水线（AiCreationPipelineServiceImpl）取代并下线；
 * 本类仅保留不落项目的两个轻量场景，事实约束沿用 ai-prompts/system.md 宪法。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentGenerateServiceImpl implements AiContentGenerateService {

    /** 形态说明（进 prompt） */
    private static final Map<Integer, String> FORM_INSTRUCTIONS = Map.of(
            1, "图文文章：输出 HTML 片段（可用 <h2> 小标题、<p> 段落、<ul><li> 列表），不要 <html>/<body> 外层，不要代码块围栏；篇幅 600-1200 字，首段为引入，结尾可带推荐商品与行动引导",
            2, "朋友圈文案：200 字以内口语化短文案，可带 1-2 个 emoji 与 1 个话题标签，适合直接复制转发",
            3, "视频脚本：60-90 秒口播，按「【画面】…\n【口播】…\n【字幕】…」分镜组织，3-4 个分镜",
            4, "小红书笔记：600-800 字纯文本，核心观点用 Emoji 列表符号，结尾 #标签段（≥5 个），关键数据用 <strong> 加粗");

    /** 风格档位说明（进 prompt） */
    private static final Map<String, String> STYLE_INSTRUCTIONS = Map.of(
            "professional", "专业科普风格：用词严谨、逻辑清晰、多用数据与术语，面向对养老品质有要求的家庭决策者",
            "warm", "温情软文风格：以长辈/家庭的真实生活场景切入，情感细腻、语气温暖，引发共鸣",
            "authoritative", "权威报告风格：结论先行、分点论述、数据化表达，塑造平台专业可信形象",
            "colloquial", "口语化风格：短句、亲切、像朋友聊天，适合朋友圈阅读");

    /** 转换温度（形态转换要求忠实，低于创作温度） */
    private static final double CONVERT_TEMPERATURE = 0.4;

    /** 选题温度（求多样性，高于创作温度） */
    private static final double TOPIC_TEMPERATURE = 0.8;

    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigGoodsService channelConfigGoodsService;
    private final AiMaterialAssembler materialAssembler;
    private final SystemConfigService systemConfigService;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    /** 统一 chat（ai-prompts/system.md 为系统提示，与流水线共用事实宪法） */
    private String chat(String prompt, double temperature) {
        return bailianChatClient.chat(
                requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                AiPrompts.load("system"), prompt, temperature);
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
        return parseTopics(chat(prompt, TOPIC_TEMPERATURE));
    }

    @Override
    public AiGenerateResultVO convert(AiConvertDTO dto) {
        String formInstruction = FORM_INSTRUCTIONS.get(dto.getTargetContentType());
        if (formInstruction == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标形态取值 1-4");
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
        return parseAnswer(dto.getTargetContentType(), chat(sb.toString(), CONVERT_TEMPERATURE), dto.getTitle());
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
