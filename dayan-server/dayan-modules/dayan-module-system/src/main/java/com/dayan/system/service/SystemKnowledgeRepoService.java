package com.dayan.system.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.system.dto.SystemKnowledgeChatDTO;
import com.dayan.system.dto.SystemKnowledgeDocImportDTO;
import com.dayan.system.dto.SystemKnowledgeRepoCreateDTO;
import com.dayan.system.dto.SystemKnowledgeRepoQueryDTO;
import com.dayan.system.dto.SystemKnowledgeRepoUpdateDTO;
import com.dayan.system.dto.SystemDocTagsDTO;
import com.dayan.system.entity.SystemKnowledgeRepo;
import com.dayan.system.vo.SystemCategoryVO;
import com.dayan.system.vo.SystemKnowledgeChatVO;
import com.dayan.system.vo.SystemKnowledgeDocVO;
import com.dayan.system.vo.SystemKnowledgeRepoTreeNodeVO;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识仓库服务：仓库与百炼远端同步 + 文档管理（上传/解析/导入索引）+ RAG 问答。
 */
public interface SystemKnowledgeRepoService {

    /** 仓库分页（平台/渠道筛选） */
    PageResult<SystemKnowledgeRepoVO> page(SystemKnowledgeRepoQueryDTO query);

    /** 仓库详情 */
    SystemKnowledgeRepoVO getDetail(Long id);

    /**
     * 创建仓库（mode=create：先落本地，上传首个文档解析成功后由 initIndex 在百炼建库；
     * mode=bind 绑定已有 IndexId）。一个渠道（或平台）仅允许一个仓库。
     */
    Long create(SystemKnowledgeRepoCreateDTO dto);

    /**
     * 懒建库：用已解析文件在百炼创建知识库并提交索引构建（CreateIndex + SubmitIndexJob），
     * 成功后 indexId/buildJobId 落库。返回构建任务 JobId。
     */
    String initIndex(Long id, List<String> fileIds);

    /** 更新仓库（仅名称/描述/排序） */
    void update(Long id, SystemKnowledgeRepoUpdateDTO dto);

    /** 删除仓库（先删百炼远端索引，成功后再删本地记录） */
    void delete(Long id);

    /** 同步远端：刷新文档数与状态 */
    void sync(Long id);

    /** 查询建库索引构建任务状态（FINISH 后置 status=1） */
    String getBuildStatus(Long id);

    /** 文档列表（实时代理百炼 ListIndexDocuments） */
    List<SystemKnowledgeDocVO> listDocuments(Long id, int pageNumber, int pageSize,
                                       String documentName, String documentStatus);

    /** 类目列表（业务空间级全量平铺） */
    List<SystemCategoryVO> listCategories();

    /** 新增类目，返回 CategoryId */
    String addCategory(String categoryName, String parentCategoryId);

    /** 删除类目（类目下有文件时百炼拒绝） */
    void deleteCategory(String categoryId);

    /** 更新文件标签（≤10，空=清空） */
    void updateDocTags(Long id, String fileId, SystemDocTagsDTO dto);

    /** 删除数据中心文件（百炼数据管理-文件；不影响已建知识库） */
    void deleteDataCenterFile(String fileId);

    /** 上传文档（可选类目/解析器/标签；categoryId 空=default，parser 空=智能解析） */
    String uploadDocument(Long id, MultipartFile file, String categoryId, String parser, List<String> tags);

    /** 查询文件解析状态（DescribeFile） */
    SystemKnowledgeDocVO getDocumentParseStatus(Long id, String fileId);

    /** 已解析文档导入索引，返回任务 JobId */
    String importDocuments(Long id, SystemKnowledgeDocImportDTO dto);

    /** 查询文档导入索引任务状态（GetIndexJobStatus） */
    String getImportStatus(Long id, String jobId);

    /** 删除索引内文档（远端永久删除） */
    void deleteDocument(Long id, String fileId);

    /** 分页查询文档切片列表（切片管理用；fileId 必填，实时代理百炼） */
    com.dayan.common.aliyun.bailian.BailianKnowledgeClient.ChunkPage listChunks(Long id, String fileId, int pageNum, int pageSize);

    /** Agent 端：当前渠道可见仓库（平台库 + 本渠道库，按排序） */
    List<SystemKnowledgeRepoVO> listForAgent(String channelCode);

    /** 按渠道编码查本渠道仓库（repo_type=2 且 channel_code 匹配；未创建返回 null） */
    SystemKnowledgeRepoVO getByChannelCode(String channelCode);

    /**
     * 渠道树形知识库（root + 全部后代）。
     *
     * <p>每节点解析独立仓库与沿祖先链最近继承源；跳过租户拦截批量查仓库，
     * 可见性以「root 及其后代」的渠道树范围为准（调用方保证 root 是当前渠道或其后代）。
     */
    List<SystemKnowledgeRepoTreeNodeVO> getRepoTree(String rootChannelCode);

    /**
     * 校验仓库对当前登录渠道可见（当前渠道 ∪ 祖先 ∪ 后代），返回仓库实体。
     *
     * <p>用于 chat/retrieve 等「使用」操作：channel 端可对继承库（祖先渠道的库）问答，
     * 可对本渠道及后代渠道的库问答；不可见时抛 NOT_FOUND。未登录渠道上下文（admin）放行。
     */
    SystemKnowledgeRepo requireRepoVisible(Long id);

    /**
     * 人物绑定路径的可见性校验（aichat 聊天专用）：
     * 平台库对渠道端放行（admin 全局绑定生效），渠道库维持归属/祖先/后代校验。
     * 无渠道上下文（admin 端）全部放行。
     */
    SystemKnowledgeRepo requireRepoVisibleForPersona(Long id);

    /** 渠道可补充的知识库：本渠道 + 全部后代渠道名下的渠道库（不含平台库），用于渠道问答人物补充下拉与保存校验 */
    List<SystemKnowledgeRepoVO> listChannelScopeRepos(String channelCode);

    /** RAG 问答（检索命中 + 大模型生成） */
    SystemKnowledgeChatVO chat(Long id, SystemKnowledgeChatDTO dto);

    /** 检索测试（仅返回命中片段，不调模型） */
    List<SystemKnowledgeChatVO.Citation> retrieve(Long id, String query, Integer topK);

    /** 按文档 ID 过滤检索（勾选文档精准召回；documentIds 须属于该仓库） */
    List<SystemKnowledgeChatVO.Citation> retrieveByDocuments(Long id, String query, Integer topK, List<String> documentIds);
}
