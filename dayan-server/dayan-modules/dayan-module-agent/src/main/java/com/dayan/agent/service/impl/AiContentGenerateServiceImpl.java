package com.dayan.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dayan.agent.dto.AiGenerateDTO;
import com.dayan.agent.service.AiContentGenerateService;
import com.dayan.agent.vo.AiGenerateResultVO;
import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import com.dayan.knowledge.service.KnowledgeRepoService;
import com.dayan.knowledge.vo.KnowledgeChatVO;
import com.dayan.knowledge.vo.KnowledgeRepoVO;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 内容生成编排实现。
 *
 * <p>素材聚合规则：范文全文（去 HTML 截断 3000 字）→ 平台库 + 本渠道库 RAG 检索
 * （检索词 = 主题 + 勾选文档名，强制召回勾选文档）→ 勾选商品详情（渠道白名单校验）。
 * 全部素材分节进 prompt，仅允许基于素材写作（防幻觉）；输出按
 * 【标题】/【摘要】/【正文】标记解析，解析失败兜底整段为正文。
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

    /** 参考范文正文最大截取字符数 */
    private static final int REF_CONTENT_MAX = 3000;

    private final KnowledgeRepoService knowledgeRepoService;
    private final ContentInfoService contentInfoService;
    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigContentService channelConfigContentService;
    private final ChannelConfigGoodsService channelConfigGoodsService;
    private final SystemConfigService systemConfigService;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    @Override
    public AiGenerateResultVO generate(AiGenerateDTO dto) {
        String formInstruction = FORM_INSTRUCTIONS.get(dto.getContentType());
        if (formInstruction == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容形态取值 1-3");
        }
        String styleInstruction = STYLE_INSTRUCTIONS.get(dto.getStyleCode());
        if (styleInstruction == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的写作风格: " + dto.getStyleCode());
        }
        String channelCode = ContextHolder.getChannelCode();
        List<String> warnings = new ArrayList<>();

        // ---------- 1. 素材聚合 ----------
        StringBuilder material = new StringBuilder();
        int materialCount = 0;

        // 1.1 参考范文（渠道可见性校验）
        if (StrUtil.isNotBlank(dto.getRefContentCode())) {
            ContentInfoVO ref = loadVisibleContent(channelCode, dto.getRefContentCode());
            material.append("【参考范文】标题：").append(ref.getTitle()).append('\n')
                    .append(stripHtml(ref.getContentBody()))
                    .append("\n\n");
            materialCount++;
        }

        // 1.2 知识库 RAG（平台库 + 本渠道库；勾选文档名并入检索词强制召回）
        List<String> selectedNames = resolveKbFileNames(channelCode, dto.getKbFileIds());
        boolean kbUsed = false;
        boolean kbSearched = false;
        String searchQuery = buildSearchQuery(dto.getTopic(), selectedNames);
        List<KnowledgeRepoVO> repos = knowledgeRepoService.listForAgent(channelCode);
        if (StrUtil.isNotBlank(searchQuery)) {
            for (KnowledgeRepoVO repo : repos) {
                if (StrUtil.isBlank(repo.getIndexId())) {
                    if (repo.getRepoType() != null && repo.getRepoType() == 2) {
                        warnings.add("本渠道知识库尚未建库，本次未使用知识库素材");
                    }
                    continue;
                }
                kbSearched = true;
                List<KnowledgeChatVO.Citation> cites = knowledgeRepoService.retrieve(repo.getId(), searchQuery, 6);
                if (!cites.isEmpty()) {
                    material.append("【知识库资料 · ").append(repo.getRepoName()).append("】\n");
                    for (int i = 0; i < cites.size(); i++) {
                        String text = StrUtil.cleanBlank(cites.get(i).getText());
                        if (StrUtil.isNotBlank(text)) {
                            material.append('[').append(i + 1).append("] ").append(text).append('\n');
                        }
                    }
                    material.append('\n');
                    kbUsed = true;
                    materialCount++;
                }
            }
        }
        if (!kbSearched) {
            warnings.add("知识库未检索到素材，未使用知识库资料");
        }

        // 1.3 商品（渠道白名单校验）
        if (dto.getGoodsCodes() != null && !dto.getGoodsCodes().isEmpty()) {
            Set<String> whitelist = channelConfigGoodsService.listByChannel(channelCode).stream()
                    .map(ChannelConfigGoods::getGoodsCode).collect(Collectors.toSet());
            material.append("【商品素材】\n");
            for (String goodsCode : dto.getGoodsCodes()) {
                if (!whitelist.contains(goodsCode)) {
                    throw new BusinessException(ErrorCode.BUSINESS, "商品不在可购范围: " + goodsCode);
                }
                GoodsInfoVO g = goodsInfoService.getDetail(goodsCode);
                material.append("- ").append(g.getGoodsName())
                        .append(g.getSummary() == null ? "" : "：" + g.getSummary())
                        .append("；价格 ")
                        .append(g.getSalePrice() == null ? "面议" : g.getSalePrice() + (g.getPriceUnit() == null ? "" : " " + g.getPriceUnit()))
                        .append('\n');
            }
            material.append('\n');
            materialCount++;
        }

        if (materialCount == 0) {
            warnings.add("未提供任何素材，生成内容可能失真，请结合知识库核对后使用");
        }

        // ---------- 2. 拼 prompt 并调用 ----------
        String userPrompt = buildUserPrompt(dto, formInstruction, styleInstruction, material.toString());
        String answer = bailianChatClient.chat(
                requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                SYSTEM_PROMPT, userPrompt);

        // ---------- 3. 解析输出 ----------
        AiGenerateResultVO result = parseAnswer(dto.getContentType(), answer, dto.getTopic());
        result.setWarnings(warnings);
        return result;
    }

    /** 系统提示词：防幻觉铁律 */
    private static final String SYSTEM_PROMPT = """
            你是「大雁养老」的资深内容创作助手，帮助保险/养老代理人撰写获客营销内容。
            铁律：
            1. 所有事实（权益档位、价格、机构名、数据、产品信息）只能来自用户消息的【素材】部分，素材没有的信息一律不得编造；
            2. 不虚构任何机构名称、价格、日期、政策；
            3. 语言自然流畅，避免空泛套话（如"综上所述""赋能""值得一提的是"），避免堆砌形容词；
            4. 全文简体中文，面向 40-70 岁客户及其子女，通俗易懂。""";

    private String buildUserPrompt(AiGenerateDTO dto, String formInstruction, String styleInstruction, String material) {
        StringBuilder sb = new StringBuilder();
        sb.append("【写作任务】\n");
        sb.append("形态：").append(formInstruction).append('\n');
        sb.append("风格：").append(styleInstruction).append('\n');
        sb.append("主题：").append(StrUtil.isNotBlank(dto.getTopic()) ? dto.getTopic() : "（缺省，请从素材归纳一个具体主题）").append('\n');
        sb.append("\n【输出格式】\n严格按以下标记输出，不要输出其他内容：\n【标题】xxx\n【摘要】xxx\n【正文】xxx\n\n");
        sb.append("【素材】\n").append(StrUtil.isBlank(material) ? "（无素材，请基于养老行业常识谨慎写作，并避免具体数字）" : material);
        return sb.toString();
    }

    /** 解析模型输出：按标记拆 标题/摘要/正文，缺标题时按形态兜底 */
    private AiGenerateResultVO parseAnswer(int contentType, String answer, String topic) {
        AiGenerateResultVO vo = new AiGenerateResultVO();
        vo.setContentType(contentType);
        String title = extract(answer, "【标题】");
        String summary = extract(answer, "【摘要】");
        String body = extract(answer, "【正文】");
        if (StrUtil.isBlank(body)) {
            // 解析失败兜底：整段作为正文（清理标记残留）
            body = answer.replaceAll("【标题】.*?\\n", "").replaceAll("【摘要】.*?\\n", "").trim();
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
        return vo;
    }

    /** 输出标记（extract 截断边界用） */
    private static final List<String> OUTPUT_MARKERS = List.of("【标题】", "【摘要】", "【正文】");

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

    /** 参考范文渠道可见性校验（appType=agent 且已配置） */
    private ContentInfoVO loadVisibleContent(String channelCode, String contentCode) {
        List<String> codes = channelConfigContentService.listByChannel(channelCode).stream()
                .filter(c -> "agent".equals(c.getAppType()))
                .map(ChannelConfigContent::getContentCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
        if (!codes.contains(contentCode)) {
            throw new BusinessException(ErrorCode.BUSINESS, "参考内容不在当前渠道可配置范围");
        }
        ContentInfoVO vo = contentInfoService.getDetail(contentCode);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参考内容不存在");
        }
        return vo;
    }

    /** 勾选文档 fileId → 文件名（跨可见库查列表映射） */
    private List<String> resolveKbFileNames(String channelCode, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        Set<String> target = Set.copyOf(fileIds);
        List<String> names = new ArrayList<>();
        for (KnowledgeRepoVO repo : knowledgeRepoService.listForAgent(channelCode)) {
            if (StrUtil.isBlank(repo.getIndexId())) {
                continue;
            }
            knowledgeRepoService.listDocuments(repo.getId(), 1, 100, null, null).stream()
                    .filter(d -> d.getFileId() != null && target.contains(d.getFileId()))
                    .forEach(d -> names.add(d.getFileName()));
        }
        return names;
    }

    /** 检索词：主题 + 勾选文档名拼接（空时返回空串，跳过知识库检索） */
    private String buildSearchQuery(String topic, List<String> selectedNames) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(topic)) {
            sb.append(topic);
        }
        for (String name : selectedNames) {
            if (name != null && !name.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(name);
            }
        }
        return sb.toString();
    }

    /** HTML 去标签（正文素材用，保留纯文本） */
    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return StrUtil.maxLength(text, REF_CONTENT_MAX);
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
