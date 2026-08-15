package com.dayan.knowledge.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.knowledge.dto.KnowledgeChatDTO;
import com.dayan.knowledge.dto.KnowledgeDocImportDTO;
import com.dayan.knowledge.dto.KnowledgeRepoCreateDTO;
import com.dayan.knowledge.dto.KnowledgeRepoQueryDTO;
import com.dayan.knowledge.dto.KnowledgeRepoUpdateDTO;
import com.dayan.knowledge.vo.KnowledgeChatVO;
import com.dayan.knowledge.vo.KnowledgeDocVO;
import com.dayan.knowledge.vo.KnowledgeRepoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识仓库服务：仓库与百炼远端同步 + 文档管理（上传/解析/导入索引）+ RAG 问答。
 */
public interface KnowledgeRepoService {

    /** 仓库分页（平台/渠道筛选） */
    PageResult<KnowledgeRepoVO> page(KnowledgeRepoQueryDTO query);

    /** 仓库详情 */
    KnowledgeRepoVO getDetail(Long id);

    /**
     * 创建仓库（mode=create：先落本地，上传首个文档解析成功后由 initIndex 在百炼建库；
     * mode=bind 绑定已有 IndexId）。一个渠道（或平台）仅允许一个仓库。
     */
    Long create(KnowledgeRepoCreateDTO dto);

    /**
     * 懒建库：用已解析文件在百炼创建知识库并提交索引构建（CreateIndex + SubmitIndexJob），
     * 成功后 indexId/buildJobId 落库。返回构建任务 JobId。
     */
    String initIndex(Long id, List<String> fileIds);

    /** 更新仓库（仅名称/描述/排序） */
    void update(Long id, KnowledgeRepoUpdateDTO dto);

    /** 删除仓库（先删百炼远端索引，成功后再删本地记录） */
    void delete(Long id);

    /** 同步远端：刷新文档数与状态 */
    void sync(Long id);

    /** 查询建库索引构建任务状态（FINISH 后置 status=1） */
    String getBuildStatus(Long id);

    /** 文档列表（实时代理百炼 ListIndexDocuments） */
    List<KnowledgeDocVO> listDocuments(Long id, int pageNumber, int pageSize,
                                       String documentName, String documentStatus);

    /** 上传文档：申请租约 → 直传 OSS → 导入解析，返回 FileId（解析异步） */
    String uploadDocument(Long id, MultipartFile file);

    /** 查询文件解析状态（DescribeFile） */
    KnowledgeDocVO getDocumentParseStatus(Long id, String fileId);

    /** 已解析文档导入索引，返回任务 JobId */
    String importDocuments(Long id, KnowledgeDocImportDTO dto);

    /** 查询文档导入索引任务状态（GetIndexJobStatus） */
    String getImportStatus(Long id, String jobId);

    /** 删除索引内文档（远端永久删除） */
    void deleteDocument(Long id, String fileId);

    /** RAG 问答（检索命中 + 大模型生成） */
    KnowledgeChatVO chat(Long id, KnowledgeChatDTO dto);

    /** 检索测试（仅返回命中片段，不调模型） */
    List<KnowledgeChatVO.Citation> retrieve(Long id, String query, Integer topK);
}
