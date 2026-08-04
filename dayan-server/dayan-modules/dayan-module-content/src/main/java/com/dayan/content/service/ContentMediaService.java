package com.dayan.content.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentMediaCreateDTO;
import com.dayan.content.dto.ContentMediaQueryDTO;
import com.dayan.content.dto.ContentMediaUpdateDTO;
import com.dayan.content.vo.ContentMediaVO;

import java.util.List;

/**
 * 内容多媒体资源服务。
 *
 * <p>按 {@code contentCode} 维护资源明细；{@code content_media} 平台共享表，不受渠道隔离。
 */
public interface ContentMediaService {

    /**
     * 按 contentCode 分页查询。
     */
    PageResult<ContentMediaVO> page(ContentMediaQueryDTO query);

    /**
     * 按 contentCode 全量列表。
     */
    List<ContentMediaVO> listByContentCode(String contentCode);

    /**
     * 详情。
     */
    ContentMediaVO getDetail(Long id);

    /**
     * 新增资源，返回主键 id。
     */
    Long create(ContentMediaCreateDTO dto);

    /**
     * 修改资源。
     */
    void update(Long id, ContentMediaUpdateDTO dto);

    /**
     * 删除资源。
     */
    void delete(Long id);

    /**
     * 按 contentCode 批量删除。
     */
    void deleteByContentCode(String contentCode);
}
