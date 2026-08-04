package com.dayan.content.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentRecordShareCreateDTO;
import com.dayan.content.dto.ContentRecordShareQueryDTO;
import com.dayan.content.dto.ContentRecordShareUpdateDTO;
import com.dayan.content.vo.ContentRecordShareVO;

/**
 * 内容分享记录服务。
 *
 * <p>{@code content_record_share} 为分片表（ASSIGN_ID 主键），按 {@code contentCode} 归属记录。
 * 主要用于记录分享事件（前端上报）+ 管理端按 contentCode 查询/回填转化数据。
 */
public interface ContentRecordShareService {

    /**
     * 按 contentCode 分页查询。
     */
    PageResult<ContentRecordShareVO> page(ContentRecordShareQueryDTO query);

    /**
     * 详情。
     */
    ContentRecordShareVO getDetail(Long id);

    /**
     * 记录分享事件，返回生成的记录 id（ASSIGN_ID）。
     */
    Long create(ContentRecordShareCreateDTO dto);

    /**
     * 回填点击/转化数据。
     */
    void update(Long id, ContentRecordShareUpdateDTO dto);

    /**
     * 删除记录。
     */
    void delete(Long id);
}
