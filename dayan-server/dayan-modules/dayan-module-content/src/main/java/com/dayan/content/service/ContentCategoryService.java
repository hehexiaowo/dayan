package com.dayan.content.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentCategoryCreateDTO;
import com.dayan.content.dto.ContentCategoryQueryDTO;
import com.dayan.content.dto.ContentCategoryUpdateDTO;
import com.dayan.content.vo.ContentCategoryVO;

import java.util.List;

/**
 * 内容分类服务。
 *
 * <p>{@code content_category} 为平台共享表（DayanTenantHandler 忽略）。按 {@code categoryName} 维护，
 * 支持 {@code parentCode} 表达层级（平铺为主）。
 */
public interface ContentCategoryService {

    /**
     * 分页查询。
     */
    PageResult<ContentCategoryVO> page(ContentCategoryQueryDTO query);

    /**
     * 全量列表（按 categoryName、sortOrder 排序）。
     */
    List<ContentCategoryVO> list(ContentCategoryQueryDTO query);

    /**
     * 详情。
     */
    ContentCategoryVO getDetail(Long id);

    /**
     * 新增分类，返回主键 id。
     */
    Long create(ContentCategoryCreateDTO dto);

    /**
     * 修改分类。
     */
    void update(Long id, ContentCategoryUpdateDTO dto);

    /**
     * 删除分类。
     */
    void delete(Long id);
}
