package com.dayan.content.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentRecordReadCreateDTO;
import com.dayan.content.dto.ContentRecordReadQueryDTO;
import com.dayan.content.vo.ContentReadStatsVO;
import com.dayan.content.vo.ContentRecordReadVO;

/**
 * 内容阅读记录服务。
 *
 * <p>{@code content_record_read} 为分片表（ASSIGN_ID 主键），按 {@code contentCode} 统计：
 * <ul>
 *   <li>PV = 阅读记录总条数</li>
 *   <li>UV = 按 {@code readerCode} 去重的访客数</li>
 * </ul>
 */
public interface ContentRecordReadService {

    /**
     * 按 contentCode 分页查询阅读记录。
     */
    PageResult<ContentRecordReadVO> page(ContentRecordReadQueryDTO query);

    /**
     * 记录阅读事件（前端上报），返回生成的记录 id（ASSIGN_ID）。
     */
    Long create(ContentRecordReadCreateDTO dto);

    /**
     * 删除阅读记录。
     */
    void delete(Long id);

    /**
     * 按 contentCode 统计 UV/PV。
     */
    ContentReadStatsVO stats(String contentCode);
}
