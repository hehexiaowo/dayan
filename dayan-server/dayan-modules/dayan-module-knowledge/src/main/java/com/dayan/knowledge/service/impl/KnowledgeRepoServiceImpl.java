package com.dayan.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.aliyun.BailianProperties;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.aliyun.bailian.BailianKnowledgeClient;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.channel.service.ChannelInfoService;
import com.dayan.knowledge.dto.KnowledgeChatDTO;
import com.dayan.knowledge.dto.KnowledgeDocImportDTO;
import com.dayan.knowledge.dto.KnowledgeRepoCreateDTO;
import com.dayan.knowledge.dto.KnowledgeRepoQueryDTO;
import com.dayan.knowledge.dto.KnowledgeRepoUpdateDTO;
import com.dayan.knowledge.entity.KnowledgeRepo;
import com.dayan.knowledge.mapper.KnowledgeRepoMapper;
import com.dayan.knowledge.service.KnowledgeRepoService;
import com.dayan.knowledge.vo.KnowledgeChatVO;
import com.dayan.knowledge.vo.KnowledgeDocVO;
import com.dayan.knowledge.vo.KnowledgeRepoVO;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 知识仓库服务实现。
 *
 * <p>远端操作为主、本地元数据为辅：文档与任务状态一律实时代理百炼，
 * 本地仅缓存仓库归属信息与文档数快照（sync 刷新）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRepoServiceImpl implements KnowledgeRepoService {

    /** 平台归属类型 */
    private static final int TYPE_PLATFORM = 1;
    /** 渠道归属类型 */
    private static final int TYPE_CHANNEL = 2;
    /** 仓库状态：构建中/未初始化 */
    private static final int STATUS_BUILDING = 0;
    /** 仓库状态：正常 */
    private static final int STATUS_OK = 1;
    /** 仓库状态：远端异常 */
    private static final int STATUS_ERROR = 2;
    /** 任务完成状态 */
    private static final String JOB_STATUS_FINISH = "FINISH";
    /** 任务完成状态（百炼服务端 FINISH 与 COMPLETED 两种取值） */
    private static final String JOB_STATUS_COMPLETED = "COMPLETED";
    /** 任务失败状态 */
    private static final String JOB_STATUS_FAILED = "FAILED";
    /** 默认召回片段数 */
    private static final int DEFAULT_TOP_K = 4;

    private final KnowledgeRepoMapper knowledgeRepoMapper;
    private final SystemConfigService systemConfigService;
    private final ChannelInfoService channelInfoService;
    private final CodeGenerator codeGenerator;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    // ==================== 仓库 CRUD（含远端同步） ====================

    @Override
    public PageResult<KnowledgeRepoVO> page(KnowledgeRepoQueryDTO query) {
        LambdaQueryWrapper<KnowledgeRepo> wrapper = new LambdaQueryWrapper<KnowledgeRepo>()
                .eq(query.getRepoType() != null, KnowledgeRepo::getRepoType, query.getRepoType())
                .eq(StrUtil.isNotBlank(query.getChannelCode()), KnowledgeRepo::getChannelCode, query.getChannelCode())
                .eq(query.getStatus() != null, KnowledgeRepo::getStatus, query.getStatus())
                .like(StrUtil.isNotBlank(query.getRepoName()), KnowledgeRepo::getRepoName, query.getRepoName())
                .orderByAsc(KnowledgeRepo::getSortOrder)
                .orderByDesc(KnowledgeRepo::getId);
        Page<KnowledgeRepo> page = knowledgeRepoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<KnowledgeRepoVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public KnowledgeRepoVO getDetail(Long id) {
        return toVO(requireRepo(id));
    }

    @Override
    public List<KnowledgeRepoVO> listForAgent(String channelCode) {
        LambdaQueryWrapper<KnowledgeRepo> wrapper = new LambdaQueryWrapper<KnowledgeRepo>()
                .eq(KnowledgeRepo::getRepoType, TYPE_PLATFORM)
                .or(w -> w.eq(KnowledgeRepo::getRepoType, TYPE_CHANNEL)
                        .eq(KnowledgeRepo::getChannelCode, channelCode))
                .orderByAsc(KnowledgeRepo::getSortOrder)
                .orderByDesc(KnowledgeRepo::getId);
        return knowledgeRepoMapper.selectList(wrapper).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(KnowledgeRepoCreateDTO dto) {
        // 归属唯一性：平台/渠道各自仅允许一个仓库
        Long existed = knowledgeRepoMapper.selectCount(new LambdaQueryWrapper<KnowledgeRepo>()
                .eq(KnowledgeRepo::getRepoType, dto.getRepoType())
                .eq(dto.getRepoType() == TYPE_CHANNEL, KnowledgeRepo::getChannelCode, dto.getChannelCode()));
        if (existed != null && existed > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    dto.getRepoType() == TYPE_CHANNEL ? "该渠道已存在知识仓库，一个渠道仅允许一个" : "平台知识仓库已存在");
        }
        if (dto.getRepoType() == TYPE_CHANNEL && StrUtil.isBlank(dto.getChannelCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道类型仓库必须指定渠道");
        }
        if (!Objects.equals(dto.getRepoType(), TYPE_PLATFORM) && !Objects.equals(dto.getRepoType(), TYPE_CHANNEL)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仓库归属类型仅支持 1=平台 2=渠道");
        }

        boolean bind = "bind".equalsIgnoreCase(dto.getMode());
        String indexId;
        String buildJobId = null;
        if (bind) {
            if (StrUtil.isBlank(dto.getIndexId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "绑定模式必须填写百炼索引 ID");
            }
            indexId = dto.getIndexId();
        } else {
            // 懒建库：百炼要求 CreateIndex 必须携带已解析文件，故先落本地，
            // 待上传首个文档解析成功后由 initIndex 在云端建库（indexId 暂空）
            indexId = null;
        }

        KnowledgeRepo repo = new KnowledgeRepo();
        repo.setRepoCode(codeGenerator.generate("KB"));
        repo.setRepoName(dto.getRepoName());
        repo.setRepoType(dto.getRepoType());
        repo.setChannelCode(dto.getRepoType() == TYPE_CHANNEL ? dto.getChannelCode() : null);
        repo.setIndexId(indexId);
        repo.setBuildJobId(buildJobId);
        repo.setDescription(dto.getDescription());
        repo.setDocCount(0);
        repo.setStatus(STATUS_BUILDING);
        repo.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        knowledgeRepoMapper.insert(repo);
        return repo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initIndex(Long id, List<String> fileIds) {
        KnowledgeRepo repo = requireRepo(id);
        if (StrUtil.isNotBlank(repo.getIndexId())) {
            throw new BusinessException(ErrorCode.BUSINESS, "仓库「" + repo.getRepoName() + "」已在百炼建库，无需重复初始化");
        }
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "初始化建库必须携带已解析的文件 ID");
        }
        BailianKnowledgeClient.CreateIndexResult result =
                requireClient().createIndex(repo.getRepoName(), repo.getDescription(), fileIds);
        repo.setIndexId(result.getIndexId());
        repo.setBuildJobId(result.getJobId());
        knowledgeRepoMapper.updateById(repo);
        log.info("懒建库完成 repoCode={} indexId={} jobId={}", repo.getRepoCode(), result.getIndexId(), result.getJobId());
        return result.getJobId();
    }

    @Override
    public void update(Long id, KnowledgeRepoUpdateDTO dto) {
        KnowledgeRepo repo = requireRepo(id);
        if (dto.getRepoName() != null) {
            repo.setRepoName(dto.getRepoName());
        }
        if (dto.getDescription() != null) {
            repo.setDescription(dto.getDescription());
        }
        if (dto.getSortOrder() != null) {
            repo.setSortOrder(dto.getSortOrder());
        }
        knowledgeRepoMapper.updateById(repo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeRepo repo = requireRepo(id);
        // 先删远端，失败则中止（避免本地删了远端成孤儿）
        if (StrUtil.isNotBlank(repo.getIndexId())) {
            requireClient().deleteIndex(repo.getIndexId());
            log.info("已删除百炼远端索引 indexId={} repoCode={}", repo.getIndexId(), repo.getRepoCode());
        }
        knowledgeRepoMapper.deleteById(id);
    }

    @Override
    public void sync(Long id) {
        KnowledgeRepo repo = requireRepo(id);
        if (StrUtil.isBlank(repo.getIndexId())) {
            throw new BusinessException(ErrorCode.BUSINESS, "仓库「" + repo.getRepoName() + "」尚未在百炼建库，上传首个文档并初始化后再同步");
        }
        try {
            BailianKnowledgeClient.DocumentPage docs =
                    requireClient().listDocuments(repo.getIndexId(), 1, 1, null, null);
            repo.setDocCount(docs.getTotal() == null ? 0 : docs.getTotal().intValue());
            repo.setStatus(STATUS_OK);
            repo.setLastSyncAt(LocalDateTime.now());
            knowledgeRepoMapper.updateById(repo);
        } catch (BusinessException e) {
            repo.setStatus(STATUS_ERROR);
            repo.setLastSyncAt(LocalDateTime.now());
            knowledgeRepoMapper.updateById(repo);
            throw e;
        }
    }

    @Override
    public String getBuildStatus(Long id) {
        KnowledgeRepo repo = requireRepo(id);
        if (StrUtil.isBlank(repo.getIndexId())) {
            return "UNBOUND";
        }
        if (StrUtil.isBlank(repo.getBuildJobId())) {
            return JOB_STATUS_FINISH;
        }
        BailianKnowledgeClient.IndexJobStatus status =
                requireClient().getIndexJobStatus(repo.getIndexId(), repo.getBuildJobId());
        if (isJobFinished(status.getJobStatus())) {
            repo.setBuildJobId(null);
            repo.setStatus(STATUS_OK);
            knowledgeRepoMapper.updateById(repo);
        } else if (JOB_STATUS_FAILED.equalsIgnoreCase(status.getJobStatus())) {
            repo.setStatus(STATUS_ERROR);
            knowledgeRepoMapper.updateById(repo);
        }
        return status.getJobStatus();
    }

    // ==================== 文档管理（远端代理） ====================

    @Override
    public List<KnowledgeDocVO> listDocuments(Long id, int pageNumber, int pageSize,
                                              String documentName, String documentStatus) {
        KnowledgeRepo repo = requireRepo(id);
        // 未建库（懒建库模式，首个文档尚未初始化）时列表为空
        if (StrUtil.isBlank(repo.getIndexId())) {
            return List.of();
        }
        BailianKnowledgeClient.DocumentPage docs = requireClient().listDocuments(
                repo.getIndexId(), pageNumber, pageSize, documentName, documentStatus);
        return docs.getDocuments().stream().map(d -> {
            KnowledgeDocVO vo = new KnowledgeDocVO();
            vo.setFileId(d.getId());
            vo.setFileName(d.getName());
            vo.setIndexStatus(d.getStatus());
            vo.setSizeInBytes(d.getSize() == null ? null : d.getSize().longValue());
            vo.setGmtModified(d.getGmtModified());
            vo.setDocumentType(d.getDocumentType());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public String uploadDocument(Long id, MultipartFile file) {
        // 懒建库模式下仓库可能尚未建库（indexId 空），上传链路不依赖索引，直接放行
        requireRepo(id);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名必须带扩展名（如 .pdf/.docx/.md）");
        }
        try {
            BailianKnowledgeClient client = requireClient();
            byte[] content = file.getBytes();
            BailianKnowledgeClient.UploadLease lease = client.applyUploadLease(fileName, content);
            BailianKnowledgeClient.uploadBinary(lease, content);
            String fileId = client.addFile(lease.getLeaseId(), null, null);
            log.info("知识库文档上传成功 repoCode={} fileName={} fileId={}", id, fileName, fileId);
            return fileId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "文件读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public KnowledgeDocVO getDocumentParseStatus(Long id, String fileId) {
        requireRepo(id);
        BailianKnowledgeClient.FileStatusInfo info = requireClient().describeFile(fileId);
        KnowledgeDocVO vo = new KnowledgeDocVO();
        vo.setFileId(info.getFileId());
        vo.setFileName(info.getFileName());
        vo.setParseStatus(info.getStatus());
        vo.setSizeInBytes(info.getSizeInBytes());
        return vo;
    }

    @Override
    public String importDocuments(Long id, KnowledgeDocImportDTO dto) {
        KnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        return requireClient().submitAddDocumentsJob(repo.getIndexId(), dto.getFileIds());
    }

    @Override
    public String getImportStatus(Long id, String jobId) {
        KnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        BailianKnowledgeClient.IndexJobStatus status =
                requireClient().getIndexJobStatus(repo.getIndexId(), jobId);
        return status.getJobStatus();
    }

    @Override
    public void deleteDocument(Long id, String fileId) {
        KnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        requireClient().deleteDocuments(repo.getIndexId(), List.of(fileId));
    }

    // ==================== RAG 问答 / 检索 ====================

    @Override
    public KnowledgeChatVO chat(Long id, KnowledgeChatDTO dto) {
        KnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        int topK = dto.getTopK() == null || dto.getTopK() < 1 ? DEFAULT_TOP_K : dto.getTopK();
        List<BailianKnowledgeClient.RetrieveNode> nodes =
                requireClient().retrieve(repo.getIndexId(), dto.getQuestion(), topK, true);
        if (nodes == null || nodes.isEmpty()) {
            return KnowledgeChatVO.builder()
                    .answer("知识库中未检索到与该问题相关的内容，请补充资料后重试或换一种问法。")
                    .citations(List.of())
                    .build();
        }

        StringBuilder context = new StringBuilder();
        List<KnowledgeChatVO.Citation> citations = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            String text = StrUtil.cleanBlank(nodes.get(i).getText());
            if (StrUtil.isBlank(text)) {
                continue;
            }
            context.append('[').append(i + 1).append("] ").append(text).append('\n');
            citations.add(KnowledgeChatVO.Citation.builder()
                    .text(text)
                    .score(nodes.get(i).getScore())
                    .build());
        }
        if (context.isEmpty()) {
            return KnowledgeChatVO.builder()
                    .answer("知识库中未检索到有效内容，请补充资料后重试。")
                    .citations(List.of())
                    .build();
        }

        String systemPrompt = """
                你是「大雁养老」的专业客服助手。请严格依据下方【知识库资料】回答用户的问题：
                1. 仅使用资料中的信息作答，不得编造资料外的内容；
                2. 若资料不足以回答，如实说明"资料中暂未找到相关内容"，并给出资料中相近的提示；
                3. 回答用简体中文，条理清晰、语气专业友善。

                【知识库资料】
                %s""".formatted(context);

        String answer = bailianChatClient.chat(
                getConfig("llm.api-key"), getConfig("llm.api-host"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                systemPrompt, dto.getQuestion());
        return KnowledgeChatVO.builder().answer(answer).citations(citations).build();
    }

    @Override
    public List<KnowledgeChatVO.Citation> retrieve(Long id, String query, Integer topK) {
        KnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "检索词不能为空");
        }
        int k = topK == null || topK < 1 ? DEFAULT_TOP_K : topK;
        return requireClient().retrieve(repo.getIndexId(), query, k, true).stream()
                .map(n -> KnowledgeChatVO.Citation.builder()
                        .text(StrUtil.cleanBlank(n.getText()))
                        .score(n.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeChatVO.Citation> retrieveByDocuments(Long id, String query, Integer topK, List<String> documentIds) {
        KnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "检索词不能为空");
        }
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        int k = topK == null || topK < 1 ? DEFAULT_TOP_K : topK;
        return requireClient().retrieve(repo.getIndexId(), query, k, true, documentIds).stream()
                .map(n -> KnowledgeChatVO.Citation.builder()
                        .text(StrUtil.cleanBlank(n.getText()))
                        .score(n.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 内部工具 ====================

    private boolean isJobFinished(String status) {
        return JOB_STATUS_FINISH.equalsIgnoreCase(status) || JOB_STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    private KnowledgeRepo requireRepo(Long id) {
        KnowledgeRepo repo = knowledgeRepoMapper.selectById(id);
        if (repo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        return repo;
    }

    private void requireIndexId(KnowledgeRepo repo) {
        if (StrUtil.isBlank(repo.getIndexId())) {
            throw new BusinessException(ErrorCode.BUSINESS, "仓库「" + repo.getRepoName() + "」未绑定百炼远端索引，无法执行该操作");
        }
    }

    /** 组装知识库管理客户端（凭据来自 system_config llm 分组） */
    private BailianKnowledgeClient requireClient() {
        String ak = getConfig("llm.access-key-id");
        String sk = getConfig("llm.access-key-secret");
        String ws = getConfig("llm.workspace-id");
        if (StrUtil.isBlank(ak) || StrUtil.isBlank(sk) || StrUtil.isBlank(ws)) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "百炼知识库凭据未配置完整（system_config → llm 分组：AccessKey ID/Secret、业务空间 ID）");
        }
        return new BailianKnowledgeClient(ak, sk, getConfig("llm.region"), ws);
    }

    private String getConfig(String configKey) {
        return systemConfigService.getValue("llm", configKey);
    }

    private KnowledgeRepoVO toVO(KnowledgeRepo repo) {
        KnowledgeRepoVO vo = new KnowledgeRepoVO();
        vo.setId(repo.getId());
        vo.setRepoCode(repo.getRepoCode());
        vo.setRepoName(repo.getRepoName());
        vo.setRepoType(repo.getRepoType());
        vo.setChannelCode(repo.getChannelCode());
        vo.setChannelName(resolveChannelName(repo.getChannelCode()));
        vo.setIndexId(repo.getIndexId());
        vo.setBuildJobId(repo.getBuildJobId());
        vo.setDescription(repo.getDescription());
        vo.setDocCount(repo.getDocCount());
        vo.setStatus(repo.getStatus());
        vo.setLastSyncAt(repo.getLastSyncAt());
        vo.setSortOrder(repo.getSortOrder());
        vo.setCreatedAt(repo.getCreatedAt());
        vo.setUpdatedAt(repo.getUpdatedAt());
        return vo;
    }

    /** 渠道名关联查询（失败容错为空，不阻断列表） */
    private String resolveChannelName(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return null;
        }
        try {
            return channelInfoService.getDetail(channelCode).getFullName();
        } catch (Exception e) {
            log.warn("关联渠道名失败 channelCode={}: {}", channelCode, e.getMessage());
            return null;
        }
    }
}
