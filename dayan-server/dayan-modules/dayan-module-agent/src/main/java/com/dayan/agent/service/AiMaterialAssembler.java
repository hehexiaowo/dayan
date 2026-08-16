package com.dayan.agent.service;

import cn.hutool.core.util.StrUtil;
import com.dayan.agent.model.AiRefTemplates;
import com.dayan.agent.service.AgentContentService;
import com.dayan.agent.vo.AgentContentVO;
import com.dayan.agent.vo.AiMaterialRefsVO;
import com.dayan.agent.vo.AiMaterialSourceVO;
import com.dayan.agent.vo.AiProjectVO;
import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.channel.service.ChannelConfigGoodsService;
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
import com.dayan.park.service.ParkAgentQueryService;
import com.dayan.park.vo.ParkDisplayBlockVO;
import com.dayan.park.vo.ParkFullDetailVO;
import com.dayan.park.vo.ParkInfoVO;
import com.dayan.park.vo.ParkPricingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 创作素材聚合器：范文（TPL/MY/渠道内容）→ 知识库 RAG（勾选文档精准召回/主题语义检索）
 * → 商品（渠道白名单）→ 机构摘要（park_info 结构化数据）。产出拼接素材块 + 引用溯源。
 */
@Service
@RequiredArgsConstructor
public class AiMaterialAssembler {

    /** 参考范文正文最大截取字符数 */
    public static final int REF_CONTENT_MAX = 3000;
    /** 机构展示板块单块截取 */
    private static final int PARK_BLOCK_MAX = 150;
    /** 素材拼接总量上限（超出截断并 warning） */
    private static final int MATERIAL_TOTAL_MAX = 8000;
    private static final Map<String, String> NETWORK_LABELS = Map.of(
            "vital", "活力长居", "care", "照护长居", "sojourn", "旅游短居");
    private static final Map<Integer, String> BILLING_LABELS = Map.of(
            1, "月", 2, "季", 3, "半年", 4, "年", 5, "一次性");
    private static final Map<Integer, String> CHARGE_LABELS = Map.of(
            1, "房间费", 2, "照护费", 3, "餐费", 4, "押金", 5, "设施费", 6, "服务费", 9, "其他");

    private final KnowledgeRepoService knowledgeRepoService;
    private final ContentInfoService contentInfoService;
    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigContentService channelConfigContentService;
    private final ChannelConfigGoodsService channelConfigGoodsService;
    private final AgentContentService agentContentService;
    private final ParkAgentQueryService parkAgentQueryService;

    /** 素材聚合结果 */
    public record MaterialBundle(String blocks, List<AiMaterialSourceVO> sources, int blockCount) {}

    /** 按素材引用聚合成 prompt 素材块（warnings 由调用方收集） */
    public MaterialBundle assemble(String channelCode, AiMaterialRefsVO refs, String topic, List<String> warnings) {
        StringBuilder material = new StringBuilder();
        List<AiMaterialSourceVO> sources = new ArrayList<>();
        int blocks = 0;
        // 1. 参考范文
        if (refs != null && StrUtil.isNotBlank(refs.getRefContentCode())) {
            RefContent ref = loadRefContent(refs.getRefContentCode(), channelCode);
            material.append("【参考范文】标题：").append(ref.title()).append('\n')
                    .append(ref.body()).append("\n\n");
            blocks++;
        }
        // 2. 知识库 RAG
        boolean hasDocs = refs != null && refs.getKbFileIds() != null && !refs.getKbFileIds().isEmpty();
        List<String> selectedNames = hasDocs ? resolveKbFileNames(channelCode, refs.getKbFileIds()) : List.of();
        boolean kbUsed = false;
        boolean kbSearched = false;
        List<KnowledgeRepoVO> repos = knowledgeRepoService.listForAgent(channelCode);
        if (hasDocs || StrUtil.isNotBlank(topic)) {
            for (KnowledgeRepoVO repo : repos) {
                if (StrUtil.isBlank(repo.getIndexId())) {
                    if (repo.getRepoType() != null && repo.getRepoType() == 2 && !kbSearched) {
                        warnings.add("本渠道知识库尚未建库，本次未使用渠道知识库素材");
                    }
                    continue;
                }
                List<KnowledgeChatVO.Citation> cites;
                if (hasDocs) {
                    List<String> docIds = resolveKbDocIdsInRepo(repo.getId(), repos, refs.getKbFileIds());
                    if (docIds.isEmpty()) {
                        continue;
                    }
                    String q = StrUtil.blankToDefault(topic, selectedNames.isEmpty() ? "养老" : selectedNames.get(0));
                    cites = knowledgeRepoService.retrieveByDocuments(repo.getId(), q, 8, docIds);
                } else {
                    cites = knowledgeRepoService.retrieve(repo.getId(), topic, 6);
                }
                kbSearched = true;
                if (!cites.isEmpty()) {
                    material.append("【知识库资料 · ").append(repo.getRepoName()).append("】\n");
                    for (int i = 0; i < cites.size(); i++) {
                        String text = StrUtil.cleanBlank(cites.get(i).getText());
                        if (StrUtil.isNotBlank(text)) {
                            material.append('[').append(i + 1).append("] ").append(text).append('\n');
                            sources.add(new AiMaterialSourceVO(repo.getRepoName(), StrUtil.maxLength(text, 120)));
                        }
                    }
                    material.append('\n');
                    kbUsed = true;
                    blocks++;
                }
            }
        }
        if (kbSearched && !kbUsed) {
            warnings.add("知识库未检索到相关素材，未使用知识库资料");
        }
        // 3. 商品（渠道白名单）
        if (refs != null && refs.getGoodsCodes() != null && !refs.getGoodsCodes().isEmpty()) {
            Set<String> whitelist = channelConfigGoodsService.listByChannel(channelCode).stream()
                    .map(ChannelConfigGoods::getGoodsCode).collect(Collectors.toSet());
            material.append("【商品素材】\n");
            for (String goodsCode : refs.getGoodsCodes()) {
                if (!whitelist.contains(goodsCode)) {
                    throw new BusinessException(ErrorCode.BUSINESS, "商品不在可购范围: " + goodsCode);
                }
                GoodsInfoVO g = goodsInfoService.getDetail(goodsCode);
                material.append("- ").append(g.getGoodsName())
                        .append(g.getSummary() == null ? "" : "：" + g.getSummary())
                        .append("；价格 ").append(g.getSalePrice() == null ? "面议"
                                : g.getSalePrice() + StrUtil.nullToEmpty(g.getPriceUnit()))
                        .append('\n');
            }
            material.append('\n');
            blocks++;
        }
        // 4. 机构摘要（新素材源）
        if (refs != null && refs.getParkCodes() != null) {
            for (String parkCode : refs.getParkCodes()) {
                material.append(buildParkSummary(parkCode)).append("\n\n");
                sources.add(new AiMaterialSourceVO("机构资料", parkCode));
                blocks++;
            }
        }
        if (blocks == 0) {
            warnings.add("未提供任何素材，生成内容可能失真，请结合知识库核对后使用");
        }
        String blocksText = material.toString();
        if (blocksText.length() > MATERIAL_TOTAL_MAX) {
            blocksText = blocksText.substring(0, MATERIAL_TOTAL_MAX);
            warnings.add("素材总量超过 " + MATERIAL_TOTAL_MAX + " 字已截断，建议减少勾选文档数");
        }
        return new MaterialBundle(blocksText, sources, blocks);
    }

    /** 机构结构化素材摘要（事实全部来自 park 库表，防机构信息幻觉） */
    public String buildParkSummary(String parkCode) {
        ParkFullDetailVO full = parkAgentQueryService.getFullDetail(parkCode);
        ParkInfoVO p = full.getParkInfo();
        StringBuilder sb = new StringBuilder("【机构资料 · ").append(p.getFullName()).append("】\n");
        sb.append("位置：").append(StrUtil.nullToEmpty(p.getProvince()))
                .append(StrUtil.nullToEmpty(p.getCity())).append(StrUtil.nullToEmpty(p.getDistrict()))
                .append(StrUtil.nullToEmpty(p.getAddress())).append('\n');
        List<String> tags = p.getNetworkTags() == null ? List.of() : p.getNetworkTags().stream()
                .filter(t -> t != null)
                .map(t -> NETWORK_LABELS.getOrDefault(t, t)).toList();
        if (!tags.isEmpty()) {
            sb.append("业态：").append(String.join("、", tags));
        }
        if (p.getDayanLevel() != null) {
            sb.append("｜平台评级：").append(p.getDayanLevel()).append(" 级");
        }
        if (StrUtil.isNotBlank(p.getSpecialtyTag())) {
            sb.append("｜特色：").append(p.getSpecialtyTag());
        }
        sb.append('\n');
        if (StrUtil.isNotBlank(p.getBaseDescription())) {
            sb.append("简介：").append(StrUtil.maxLength(p.getBaseDescription(), 400)).append('\n');
        }
        sb.append("床位：总 ").append(p.getTotalBeds() == null ? "-" : String.valueOf(p.getTotalBeds()))
                .append(" 张，可入住 ").append(p.getAvailableBeds() == null ? "-" : String.valueOf(p.getAvailableBeds()))
                .append(" 张");
        if (p.getCheckInAgeMin() != null && p.getCheckInAgeMax() != null) {
            sb.append("；入住年龄 ").append(p.getCheckInAgeMin()).append('-').append(p.getCheckInAgeMax()).append(" 岁");
        }
        sb.append('\n');
        if (p.getMinPriceDisplay() != null && p.getMinPriceDisplay() > 0) {
            sb.append("价格参考：").append(p.getMinPriceDisplay())
                    .append(p.getMaxPriceDisplay() != null && p.getMaxPriceDisplay() > p.getMinPriceDisplay()
                            ? "-" + p.getMaxPriceDisplay() : "")
                    .append(" 元/").append(StrUtil.nullToDefault(p.getPriceUnit(), "月")).append('\n');
        }
        appendNames(sb, "房型", full.getRoomTypes() == null ? List.of()
                : full.getRoomTypes().stream().map(r -> StrUtil.nullToDefault(r.getRoomTypeName(), "")).filter(StrUtil::isNotBlank).toList());
        if (full.getPricingList() != null && !full.getPricingList().isEmpty()) {
            List<ParkPricingVO> current = full.getPricingList().stream()
                    .filter(pr -> Integer.valueOf(1).equals(pr.getIsCurrent()))
                    .limit(8).toList();
            if (!current.isEmpty()) {
                sb.append("费用明细（当前价）：");
                for (ParkPricingVO pr : current) {
                    String refName = pr.getRefName() != null ? pr.getRefName()
                            : StrUtil.nullToDefault(pr.getPlanName(), "费用项");
                    String cycle = pr.getBillingCycle() == null ? "期"
                            : BILLING_LABELS.getOrDefault(pr.getBillingCycle(), "期");
                    sb.append(pr.getChargeType() == null ? "费用"
                                    : CHARGE_LABELS.getOrDefault(pr.getChargeType(), "费用")).append(' ')
                            .append(refName).append(' ')
                            .append(pr.getSalePrice() == null ? "面议" : pr.getSalePrice() + "元/")
                            .append(cycle).append("；");
                }
                sb.append('\n');
            }
        }
        appendNames(sb, "服务", names(full.getServiceTypes(), "getServiceTypeName"));
        appendNames(sb, "照护等级", names(full.getCareTypes(), "getCareTypeName"));
        appendNames(sb, "餐饮", names(full.getFoodTypes(), "getFoodTypeName"));
        appendNames(sb, "设施", names(full.getFacilityTypes(), "getFacilityTypeName"));
        if (full.getDisplayBlocks() != null) {
            List<ParkDisplayBlockVO> blocks = full.getDisplayBlocks().stream().limit(4).toList();
            for (ParkDisplayBlockVO b : blocks) {
                if (StrUtil.isNotBlank(b.getBlockTitle())) {
                    sb.append("亮点：").append(b.getBlockTitle()).append("——")
                            .append(StrUtil.maxLength(stripHtml(b.getContent()), PARK_BLOCK_MAX)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    /** 详情页素材名回显（安全降级：任一失败只回退为空） */
    public void resolveRefNames(AiProjectVO vo) {
        AiMaterialRefsVO refs = vo.getMaterialRefs();
        if (refs == null) {
            return;
        }
        String channelCode = StrUtil.nullToEmpty(ContextHolder.getChannelCode());
        if (StrUtil.isNotBlank(refs.getRefContentCode())) {
            try {
                vo.setRefContentName(loadRefContent(refs.getRefContentCode(), channelCode).title());
            } catch (Exception ignored) {
                vo.setRefContentName(refs.getRefContentCode());
            }
        }
        if (refs.getKbFileIds() != null && !refs.getKbFileIds().isEmpty()) {
            try {
                vo.setKbFileNames(resolveKbFileNames(channelCode, refs.getKbFileIds()));
            } catch (Exception ignored) {
                vo.setKbFileNames(List.of());
            }
        }
        if (refs.getGoodsCodes() != null) {
            vo.setGoodsNames(refs.getGoodsCodes().stream().map(c -> {
                try {
                    return goodsInfoService.getDetail(c).getGoodsName();
                } catch (Exception e) {
                    return c;
                }
            }).toList());
        }
        if (refs.getParkCodes() != null) {
            vo.setParkNames(refs.getParkCodes().stream().map(c -> {
                try {
                    return parkAgentQueryService.getPublishedDetail(c).getFullName();
                } catch (Exception e) {
                    return c;
                }
            }).toList());
        }
    }

    // ---------- 以下私有方法从 AiContentGenerateServiceImpl 原样迁移 ----------

    /** 范文标题+正文（TPL:/MY:/渠道内容三分支，单次查询） */
    private record RefContent(String title, String body) {}

    private RefContent loadRefContent(String refContentCode, String channelCode) {
        if (refContentCode.startsWith("TPL:")) {
            AiRefTemplates.RefTemplate tpl = AiRefTemplates.byCode(refContentCode);
            if (tpl == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "范文模板不存在: " + refContentCode);
            }
            return new RefContent(tpl.name(), tpl.body());
        }
        if (refContentCode.startsWith("MY:")) {
            AgentContentVO my = loadMyContent(refContentCode);
            return new RefContent(my.getTitle(), stripHtml(my.getContentBody()));
        }
        ContentInfoVO ref = loadVisibleContent(channelCode, refContentCode);
        return new RefContent(ref.getTitle(), stripHtml(ref.getContentBody()));
    }

    private AgentContentVO loadMyContent(String refContentCode) {
        long id;
        try {
            id = Long.parseLong(StrUtil.removePrefix(refContentCode, "MY:"));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "我的内容范文格式错误");
        }
        return agentContentService.getDetail(id);
    }

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

    /** 勾选文档 fileId → 文件名（跨可见库） */
    public List<String> resolveKbFileNames(String channelCode, List<String> fileIds) {
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

    /** 指定仓库内被勾选文档的 docId（跨库按归属分库检索） */
    private List<String> resolveKbDocIdsInRepo(Long repoId, List<KnowledgeRepoVO> repos, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        Set<String> target = Set.copyOf(fileIds);
        return knowledgeRepoService.listDocuments(repoId, 1, 100, null, null).stream()
                .map(com.dayan.knowledge.vo.KnowledgeDocVO::getFileId)
                .filter(id -> id != null && target.contains(id))
                .collect(Collectors.toList());
    }

    public String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return StrUtil.maxLength(text, REF_CONTENT_MAX);
    }

    private void appendNames(StringBuilder sb, String label, List<String> names) {
        List<String> valid = names == null ? List.of() : names.stream().filter(StrUtil::isNotBlank).limit(8).toList();
        if (!valid.isEmpty()) {
            sb.append(label).append("：").append(String.join("、", valid)).append('\n');
        }
    }

    /** VO 列表取名称的反射桥（roomTypes/serviceTypes 等 VO 无公共接口，按 getter 名取） */
    private List<String> names(List<?> list, String getter) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(item -> {
            try {
                Object v = item.getClass().getMethod(getter).invoke(item);
                return v == null ? "" : v.toString();
            } catch (Exception e) {
                return "";
            }
        }).toList();
    }
}
