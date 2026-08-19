package com.dayan.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.aliyun.BailianProperties;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.aliyun.bailian.BailianKnowledgeClient;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.dto.SystemDocTagsDTO;
import com.dayan.system.dto.SystemKnowledgeChatDTO;
import com.dayan.system.dto.SystemKnowledgeDocImportDTO;
import com.dayan.system.dto.SystemKnowledgeIndexConfig;
import com.dayan.system.dto.SystemKnowledgeRepoCreateDTO;
import com.dayan.system.dto.SystemKnowledgeRepoQueryDTO;
import com.dayan.system.dto.SystemKnowledgeRepoUpdateDTO;
import com.dayan.system.entity.SystemKnowledgeRepo;
import com.dayan.system.mapper.ChannelInfoLight;
import com.dayan.system.mapper.ChannelInfoLightMapper;
import com.dayan.system.mapper.SystemKnowledgeRepoMapper;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemCategoryAddDTO;
import com.dayan.system.vo.SystemCategoryVO;
import com.dayan.system.vo.SystemKnowledgeChatVO;
import com.dayan.system.vo.SystemKnowledgeDocVO;
import com.dayan.system.vo.SystemKnowledgeRepoTreeNodeVO;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
public class SystemKnowledgeRepoServiceImpl implements SystemKnowledgeRepoService {

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

    private final SystemKnowledgeRepoMapper knowledgeRepoMapper;
    private final ChannelInfoLightMapper channelInfoLightMapper;
    private final SystemConfigService systemConfigService;
    private final CodeGenerator codeGenerator;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    // ==================== 仓库 CRUD（含远端同步） ====================

    @Override
    public PageResult<SystemKnowledgeRepoVO> page(SystemKnowledgeRepoQueryDTO query) {
        LambdaQueryWrapper<SystemKnowledgeRepo> wrapper = new LambdaQueryWrapper<SystemKnowledgeRepo>()
                .eq(query.getRepoType() != null, SystemKnowledgeRepo::getRepoType, query.getRepoType())
                .eq(StrUtil.isNotBlank(query.getChannelCode()), SystemKnowledgeRepo::getChannelCode, query.getChannelCode())
                .eq(query.getStatus() != null, SystemKnowledgeRepo::getStatus, query.getStatus())
                .like(StrUtil.isNotBlank(query.getRepoName()), SystemKnowledgeRepo::getRepoName, query.getRepoName())
                .orderByAsc(SystemKnowledgeRepo::getSortOrder)
                .orderByDesc(SystemKnowledgeRepo::getId);
        Page<SystemKnowledgeRepo> page = knowledgeRepoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SystemKnowledgeRepoVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SystemKnowledgeRepoVO getDetail(Long id) {
        return toVO(requireRepo(id));
    }

    @Override
    public List<SystemKnowledgeRepoVO> listForAgent(String channelCode) {
        LambdaQueryWrapper<SystemKnowledgeRepo> wrapper = new LambdaQueryWrapper<SystemKnowledgeRepo>()
                .eq(SystemKnowledgeRepo::getRepoType, TYPE_PLATFORM)
                .or(w -> w.eq(SystemKnowledgeRepo::getRepoType, TYPE_CHANNEL)
                        .eq(SystemKnowledgeRepo::getChannelCode, channelCode))
                .orderByAsc(SystemKnowledgeRepo::getSortOrder)
                .orderByDesc(SystemKnowledgeRepo::getId);
        return knowledgeRepoMapper.selectList(wrapper).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SystemKnowledgeRepoVO getByChannelCode(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return null;
        }
        SystemKnowledgeRepo repo = knowledgeRepoMapper.selectOne(new LambdaQueryWrapper<SystemKnowledgeRepo>()
                .eq(SystemKnowledgeRepo::getRepoType, TYPE_CHANNEL)
                .eq(SystemKnowledgeRepo::getChannelCode, channelCode)
                .last("LIMIT 1"));
        return repo == null ? null : toVO(repo);
    }

    @Override
    public List<SystemKnowledgeRepoTreeNodeVO> getRepoTree(String rootChannelCode) {
        List<ChannelInfoLight> channels = channelInfoLightMapper.selectAll();
        if (channels.isEmpty()) {
            return List.of();
        }
        Map<String, ChannelInfoLight> byCode = channels.stream()
                .collect(Collectors.toMap(ChannelInfoLight::getChannelCode, Function.identity(), (a, b) -> a));

        // 收集 root 及其全部后代（ancestors 链包含 root）；root 为空 = 全渠道树
        Set<String> scope = new HashSet<>();
        for (ChannelInfoLight c : channels) {
            if (StrUtil.isBlank(rootChannelCode) || rootChannelCode.equals(c.getChannelCode())
                    || (c.getAncestors() != null && c.getAncestors().contains(rootChannelCode))) {
                scope.add(c.getChannelCode());
            }
        }
        // 继承解析需要祖先链上的仓库（scope 渠道的 ancestors 一并纳入查询范围）
        Set<String> queryCodes = new HashSet<>(scope);
        for (String code : scope) {
            ChannelInfoLight c = byCode.get(code);
            if (c != null && StrUtil.isNotBlank(c.getAncestors())) {
                for (String anc : c.getAncestors().split(",")) {
                    if (StrUtil.isNotBlank(anc)) {
                        queryCodes.add(anc);
                    }
                }
            }
        }
        Map<String, SystemKnowledgeRepo> repoByChannel = new HashMap<>();
        for (SystemKnowledgeRepo r : knowledgeRepoMapper.selectByChannelCodes(queryCodes)) {
            repoByChannel.putIfAbsent(r.getChannelCode(), r);
        }

        // 按祖先链从近到远找最近建有仓库的渠道（继承源）
        Map<String, SystemKnowledgeRepo> effectiveByChannel = new HashMap<>();
        for (String code : scope) {
            SystemKnowledgeRepo own = repoByChannel.get(code);
            if (own != null) {
                effectiveByChannel.put(code, own);
                continue;
            }
            ChannelInfoLight c = byCode.get(code);
            if (c != null && StrUtil.isNotBlank(c.getAncestors())) {
                String[] ancestors = c.getAncestors().split(",");
                for (int i = ancestors.length - 1; i >= 0; i--) {
                    SystemKnowledgeRepo ancRepo = repoByChannel.get(ancestors[i].trim());
                    if (ancRepo != null) {
                        effectiveByChannel.put(code, ancRepo);
                        break;
                    }
                }
            }
        }

        // 组装树：parent_code 挂接，root 为顶层
        Map<String, SystemKnowledgeRepoTreeNodeVO> nodes = new LinkedHashMap<>();
        for (ChannelInfoLight c : channels) {
            if (!scope.contains(c.getChannelCode())) {
                continue;
            }
            SystemKnowledgeRepoTreeNodeVO node = new SystemKnowledgeRepoTreeNodeVO();
            node.setChannelCode(c.getChannelCode());
            node.setFullName(c.getFullName());
            node.setShortName(c.getShortName());
            node.setLevel(c.getLevel());
            node.setRepo(repoByChannel.get(c.getChannelCode()) == null ? null : toVO(repoByChannel.get(c.getChannelCode())));
            node.setEffectiveRepo(effectiveByChannel.get(c.getChannelCode()) == null ? null
                    : toVO(effectiveByChannel.get(c.getChannelCode())));
            SystemKnowledgeRepo effective = effectiveByChannel.get(c.getChannelCode());
            if (effective != null && !c.getChannelCode().equals(effective.getChannelCode())) {
                node.setInheritedFrom(effective.getChannelCode());
                ChannelInfoLight src = byCode.get(effective.getChannelCode());
                node.setInheritedFromName(src == null ? effective.getChannelCode() : src.getShortName());
            }
            nodes.put(c.getChannelCode(), node);
        }
        // 挂 children
        List<SystemKnowledgeRepoTreeNodeVO> roots = new ArrayList<>();
        for (SystemKnowledgeRepoTreeNodeVO node : nodes.values()) {
            ChannelInfoLight c = byCode.get(node.getChannelCode());
            String parent = c == null ? null : c.getParentCode();
            if (parent != null && nodes.containsKey(parent)) {
                nodes.get(parent).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Override
    public SystemKnowledgeRepo requireRepoVisible(Long id) {
        SystemKnowledgeRepo repo = knowledgeRepoMapper.selectByIdIgnoreTenant(id);
        if (repo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        String currentCode = ContextHolder.getChannelCode();
        if (StrUtil.isBlank(currentCode)) {
            // 未绑定渠道上下文（admin 端）放行
            return repo;
        }
        String repoChannel = repo.getChannelCode();
        if (repoChannel != null && repoChannel.equals(currentCode)) {
            return repo;
        }
        // 平台库（channel_code=null）对 channel 端不可见（继承/后代均不适用）
        if (StrUtil.isBlank(repoChannel)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        // 可见范围：repo 渠道 ∈ 当前渠道的祖先（继承使用）∪ 当前渠道的后代
        List<ChannelInfoLight> channels = channelInfoLightMapper.selectAll();
        ChannelInfoLight repoChannelInfo = channels.stream()
                .filter(c -> repoChannel.equals(c.getChannelCode()))
                .findFirst().orElse(null);
        if (repoChannelInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        boolean visible = repoChannelInfo.getAncestors() != null
                && repoChannelInfo.getAncestors().contains(currentCode);
        if (!visible) {
            // repo 渠道是当前渠道的祖先（子渠道继承使用祖先的库）
            ChannelInfoLight currentInfo = channels.stream()
                    .filter(c -> currentCode.equals(c.getChannelCode()))
                    .findFirst().orElse(null);
            visible = currentInfo != null && currentInfo.getAncestors() != null
                    && currentInfo.getAncestors().contains(repoChannel);
        }
        if (!visible) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        return repo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemKnowledgeRepoCreateDTO dto) {
        // 归属唯一性：平台/渠道各自仅允许一个仓库
        Long existed = knowledgeRepoMapper.selectCount(new LambdaQueryWrapper<SystemKnowledgeRepo>()
                .eq(SystemKnowledgeRepo::getRepoType, dto.getRepoType())
                .eq(dto.getRepoType() == TYPE_CHANNEL, SystemKnowledgeRepo::getChannelCode, dto.getChannelCode()));
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

        SystemKnowledgeRepo repo = new SystemKnowledgeRepo();
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
        if (!bind && dto.getIndexConfig() != null) {
            dto.getIndexConfig().validate();
            repo.setConfigJson(JSONUtil.toJsonStr(dto.getIndexConfig()));
        }
        knowledgeRepoMapper.insert(repo);
        return repo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initIndex(Long id, List<String> fileIds) {
        SystemKnowledgeRepo repo = requireRepo(id);
        if (StrUtil.isNotBlank(repo.getIndexId())) {
            throw new BusinessException(ErrorCode.BUSINESS, "仓库「" + repo.getRepoName() + "」已在百炼建库，无需重复初始化");
        }
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "初始化建库必须携带已解析的文件 ID");
        }
        SystemKnowledgeIndexConfig config = parseConfig(repo);
        BailianKnowledgeClient.CreateIndexResult result =
                requireClient().createIndex(repo.getRepoName(), repo.getDescription(), fileIds,
                        config == null ? null : config.toQueryMap());
        repo.setIndexId(result.getIndexId());
        repo.setBuildJobId(result.getJobId());
        knowledgeRepoMapper.updateById(repo);
        log.info("懒建库完成 repoCode={} indexId={} jobId={}", repo.getRepoCode(), result.getIndexId(), result.getJobId());
        return result.getJobId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemKnowledgeRepoUpdateDTO dto) {
        SystemKnowledgeRepo repo = requireRepo(id);
        // 名称/描述变化且已建库时先同步百炼远端（失败中止，保证本地与远端一致）
        if (dto.getRepoName() != null || dto.getDescription() != null) {
            String newName = dto.getRepoName() != null ? dto.getRepoName() : repo.getRepoName();
            String newDesc = dto.getDescription() != null ? dto.getDescription() : repo.getDescription();
            if (StrUtil.isNotBlank(repo.getIndexId())
                    && (!newName.equals(repo.getRepoName()) || !java.util.Objects.equals(newDesc, repo.getDescription()))) {
                requireClient().updateIndex(repo.getIndexId(), newName, newDesc);
            }
        }
        if (dto.getRepoName() != null) {
            repo.setRepoName(dto.getRepoName());
        }
        if (dto.getDescription() != null) {
            repo.setDescription(dto.getDescription());
        }
        if (dto.getIndexConfig() != null) {
            SystemKnowledgeIndexConfig config = dto.getIndexConfig();
            config.validate();
            if (StrUtil.isBlank(repo.getIndexId())) {
                // 未建库（懒建库）：全量保存，initIndex 时应用
                repo.setConfigJson(JSONUtil.toJsonStr(config));
            } else {
                // 已建库：仅检索参数可改并同步百炼
                SystemKnowledgeIndexConfig existing = parseConfig(repo);
                assertUpdatableConfig(existing, config);
                boolean syncNeeded = (config.getDenseTopK() != null && !config.getDenseTopK().equals(existing == null ? null : existing.getDenseTopK()))
                        || (config.getSparseTopK() != null && !config.getSparseTopK().equals(existing == null ? null : existing.getSparseTopK()))
                        || (config.getRerankMinScore() != null && !config.getRerankMinScore().equals(existing == null ? null : existing.getRerankMinScore()));
                if (syncNeeded) {
                    requireClient().updateIndex(repo.getIndexId(), repo.getRepoName(), repo.getDescription(),
                            config.getDenseTopK(), config.getSparseTopK(), config.getRerankMinScore());
                }
                // 合并：保留未提交的不可变字段，覆盖可更新字段
                SystemKnowledgeIndexConfig merged = existing == null ? new SystemKnowledgeIndexConfig() : existing;
                if (config.getDenseTopK() != null) merged.setDenseTopK(config.getDenseTopK());
                if (config.getSparseTopK() != null) merged.setSparseTopK(config.getSparseTopK());
                if (config.getRerankMinScore() != null) merged.setRerankMinScore(config.getRerankMinScore());
                repo.setConfigJson(JSONUtil.toJsonStr(merged));
            }
        }
        if (dto.getSortOrder() != null) {
            repo.setSortOrder(dto.getSortOrder());
        }
        knowledgeRepoMapper.updateById(repo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SystemKnowledgeRepo repo = requireRepo(id);
        // 先删远端，失败则中止（避免本地删了远端成孤儿）
        if (StrUtil.isNotBlank(repo.getIndexId())) {
            requireClient().deleteIndex(repo.getIndexId());
            log.info("已删除百炼远端索引 indexId={} repoCode={}", repo.getIndexId(), repo.getRepoCode());
        }
        knowledgeRepoMapper.deleteById(id);
    }

    @Override
    public void sync(Long id) {
        SystemKnowledgeRepo repo = requireRepo(id);
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
        SystemKnowledgeRepo repo = requireRepo(id);
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
    public List<SystemKnowledgeDocVO> listDocuments(Long id, int pageNumber, int pageSize,
                                              String documentName, String documentStatus) {
        SystemKnowledgeRepo repo = requireRepo(id);
        // 未建库（懒建库模式，首个文档尚未初始化）时列表为空
        if (StrUtil.isBlank(repo.getIndexId())) {
            return List.of();
        }
        BailianKnowledgeClient.DocumentPage docs = requireClient().listDocuments(
                repo.getIndexId(), pageNumber, pageSize, documentName, documentStatus);
        return docs.getDocuments().stream().map(d -> {
            SystemKnowledgeDocVO vo = new SystemKnowledgeDocVO();
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
    public String uploadDocument(Long id, MultipartFile file, String categoryId, String parser, List<String> tags) {
        // 懒建库模式下仓库可能尚未建库（indexId 空），上传链路不依赖索引，直接放行
        requireRepo(id);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名必须带扩展名（如 .pdf/.docx/.md）");
        }
        if (tags != null && tags.size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签最多 10 个");
        }
        try {
            BailianKnowledgeClient client = requireClient();
            byte[] content = file.getBytes();
            BailianKnowledgeClient.UploadLease lease = client.applyUploadLease(fileName, content);
            BailianKnowledgeClient.uploadBinary(lease, content);
            String fileId = client.addFile(lease.getLeaseId(), categoryId, parser, tags);
            log.info("知识库文档上传成功 repoId={} fileName={} categoryId={} fileId={}", id, fileName,
                    categoryId == null ? "default" : categoryId, fileId);
            return fileId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "文件读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SystemKnowledgeDocVO getDocumentParseStatus(Long id, String fileId) {
        requireRepo(id);
        BailianKnowledgeClient.FileStatusInfo info = requireClient().describeFile(fileId);
        SystemKnowledgeDocVO vo = new SystemKnowledgeDocVO();
        vo.setFileId(info.getFileId());
        vo.setFileName(info.getFileName());
        vo.setParseStatus(info.getStatus());
        vo.setSizeInBytes(info.getSizeInBytes());
        vo.setCategoryId(info.getCategoryId());
        vo.setTags(info.getTags());
        vo.setParser(info.getParser());
        return vo;
    }

    @Override
    public String importDocuments(Long id, SystemKnowledgeDocImportDTO dto) {
        SystemKnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        return requireClient().submitAddDocumentsJob(repo.getIndexId(), dto.getFileIds());
    }

    @Override
    public String getImportStatus(Long id, String jobId) {
        SystemKnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        BailianKnowledgeClient.IndexJobStatus status =
                requireClient().getIndexJobStatus(repo.getIndexId(), jobId);
        return status.getJobStatus();
    }

    @Override
    public void deleteDocument(Long id, String fileId) {
        SystemKnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        requireClient().deleteDocuments(repo.getIndexId(), List.of(fileId));
    }

    @Override
    public BailianKnowledgeClient.ChunkPage listChunks(Long id, String fileId, int pageNum, int pageSize) {
        SystemKnowledgeRepo repo = requireRepo(id);
        requireIndexId(repo);
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片查询必须指定文档 ID");
        }
        return requireClient().listChunks(repo.getIndexId(), fileId, pageNum, pageSize);
    }

    // ==================== 类目与文件标签管理（实时代理百炼） ====================

    @Override
    public List<SystemCategoryVO> listCategories() {
        return requireClient().listCategories().stream().map(c -> {
            SystemCategoryVO vo = new SystemCategoryVO();
            vo.setCategoryId(c.getCategoryId());
            vo.setCategoryName(c.getCategoryName());
            vo.setParentCategoryId(c.getParentCategoryId());
            vo.setIsDefault(c.getIsDefault());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public String addCategory(String categoryName, String parentCategoryId) {
        return requireClient().addCategory(categoryName, parentCategoryId);
    }

    @Override
    public void deleteCategory(String categoryId) {
        requireClient().deleteCategory(categoryId);
    }

    @Override
    public void updateDocTags(Long id, String fileId, SystemDocTagsDTO dto) {
        requireRepo(id);
        if (dto == null || dto.getTags() == null || dto.getTags().size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签最多 10 个");
        }
        requireClient().updateFileTags(fileId, dto.getTags());
    }

    // ==================== RAG 问答 / 检索 ====================

    @Override
    public SystemKnowledgeChatVO chat(Long id, SystemKnowledgeChatDTO dto) {
        SystemKnowledgeRepo repo = requireRepoVisible(id);
        requireIndexId(repo);
        int topK = dto.getTopK() == null || dto.getTopK() < 1 ? DEFAULT_TOP_K : dto.getTopK();
        List<BailianKnowledgeClient.RetrieveNode> nodes =
                requireClient().retrieve(repo.getIndexId(), dto.getQuestion(), topK, true);
        if (nodes == null || nodes.isEmpty()) {
            return SystemKnowledgeChatVO.builder()
                    .answer("知识库中未检索到与该问题相关的内容，请补充资料后重试或换一种问法。")
                    .citations(List.of())
                    .build();
        }

        StringBuilder context = new StringBuilder();
        List<SystemKnowledgeChatVO.Citation> citations = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            String text = StrUtil.cleanBlank(nodes.get(i).getText());
            if (StrUtil.isBlank(text)) {
                continue;
            }
            context.append('[').append(i + 1).append("] ").append(text).append('\n');
            citations.add(SystemKnowledgeChatVO.Citation.builder()
                    .text(text)
                    .score(nodes.get(i).getScore())
                    .build());
        }
        if (context.isEmpty()) {
            return SystemKnowledgeChatVO.builder()
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
        return SystemKnowledgeChatVO.builder().answer(answer).citations(citations).build();
    }

    @Override
    public List<SystemKnowledgeChatVO.Citation> retrieve(Long id, String query, Integer topK) {
        SystemKnowledgeRepo repo = requireRepoVisible(id);
        requireIndexId(repo);
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "检索词不能为空");
        }
        int k = topK == null || topK < 1 ? DEFAULT_TOP_K : topK;
        return requireClient().retrieve(repo.getIndexId(), query, k, true).stream()
                .map(n -> SystemKnowledgeChatVO.Citation.builder()
                        .text(StrUtil.cleanBlank(n.getText()))
                        .score(n.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemKnowledgeChatVO.Citation> retrieveByDocuments(Long id, String query, Integer topK, List<String> documentIds) {
        SystemKnowledgeRepo repo = requireRepoVisible(id);
        requireIndexId(repo);
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "检索词不能为空");
        }
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        int k = topK == null || topK < 1 ? DEFAULT_TOP_K : topK;
        return requireClient().retrieve(repo.getIndexId(), query, k, true, documentIds).stream()
                .map(n -> SystemKnowledgeChatVO.Citation.builder()
                        .text(StrUtil.cleanBlank(n.getText()))
                        .score(n.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 内部工具 ====================

    private boolean isJobFinished(String status) {
        return JOB_STATUS_FINISH.equalsIgnoreCase(status) || JOB_STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    private SystemKnowledgeRepo requireRepo(Long id) {
        SystemKnowledgeRepo repo = knowledgeRepoMapper.selectById(id);
        if (repo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识仓库不存在: " + id);
        }
        return repo;
    }

    private void requireIndexId(SystemKnowledgeRepo repo) {
        if (StrUtil.isBlank(repo.getIndexId())) {
            throw new BusinessException(ErrorCode.BUSINESS, "仓库「" + repo.getRepoName() + "」未绑定百炼远端索引，无法执行该操作");
        }
    }

    /** 已建库配置可更新校验：仅 denseTopK/sparseTopK/rerankMinScore 可变，其余报错 */
    static void assertUpdatableConfig(SystemKnowledgeIndexConfig existing, SystemKnowledgeIndexConfig incoming) {
        if (existing == null || incoming == null) {
            return;
        }
        if (!Objects.equals(existing.getChunkMode(), incoming.getChunkMode())
                || !Objects.equals(existing.getSeparator(), incoming.getSeparator())
                || !Objects.equals(existing.getChunkSize(), incoming.getChunkSize())
                || !Objects.equals(existing.getOverlapSize(), incoming.getOverlapSize())
                || !Objects.equals(existing.getEmbeddingModel(), incoming.getEmbeddingModel())
                || !Objects.equals(existing.getRerankModel(), incoming.getRerankModel())
                || !Objects.equals(existing.getRerankMode(), incoming.getRerankMode())
                || !Objects.equals(existing.getEnableRewrite(), incoming.getEnableRewrite())) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "切分方式、向量模型、重排模型等配置在建库后不可修改（如需调整请删除仓库重建）");
        }
    }

    private SystemKnowledgeIndexConfig parseConfig(SystemKnowledgeRepo repo) {
        if (StrUtil.isBlank(repo.getConfigJson())) {
            return null;
        }
        try {
            return JSONUtil.toBean(repo.getConfigJson(), SystemKnowledgeIndexConfig.class);
        } catch (Exception e) {
            log.warn("索引配置 JSON 解析失败 repoId={}: {}", repo.getId(), e.getMessage());
            return null;
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

    private SystemKnowledgeRepoVO toVO(SystemKnowledgeRepo repo) {
        SystemKnowledgeRepoVO vo = new SystemKnowledgeRepoVO();
        vo.setId(repo.getId());
        vo.setRepoCode(repo.getRepoCode());
        vo.setRepoName(repo.getRepoName());
        vo.setRepoType(repo.getRepoType());
        vo.setChannelCode(repo.getChannelCode());
        vo.setChannelName(resolveChannelName(repo.getChannelCode()));
        vo.setChannelShortName(resolveChannelShortName(repo.getChannelCode()));
        vo.setIndexId(repo.getIndexId());
        vo.setBuildJobId(repo.getBuildJobId());
        vo.setDescription(repo.getDescription());
        vo.setDocCount(repo.getDocCount());
        vo.setStatus(repo.getStatus());
        vo.setLastSyncAt(repo.getLastSyncAt());
        vo.setSortOrder(repo.getSortOrder());
        vo.setCreatedAt(repo.getCreatedAt());
        vo.setUpdatedAt(repo.getUpdatedAt());
        vo.setIndexConfig(parseConfig(repo));
        return vo;
    }

    /** 渠道名关联查询（失败容错为空，不阻断列表；直读 channel_info 表） */
    private String resolveChannelName(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return null;
        }
        try {
            return knowledgeRepoMapper.selectChannelFullName(channelCode);
        } catch (Exception e) {
            log.warn("关联渠道名失败 channelCode={}: {}", channelCode, e.getMessage());
            return null;
        }
    }

    /** 渠道简称关联查询（列表「归属」列展示用；失败容错为空，不阻断列表） */
    private String resolveChannelShortName(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return null;
        }
        try {
            return knowledgeRepoMapper.selectChannelShortName(channelCode);
        } catch (Exception e) {
            log.warn("关联渠道简称失败 channelCode={}: {}", channelCode, e.getMessage());
            return null;
        }
    }
}
