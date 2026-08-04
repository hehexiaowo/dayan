package com.dayan.content.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentInfoAuditDTO;
import com.dayan.content.dto.ContentInfoCreateDTO;
import com.dayan.content.dto.ContentInfoQueryDTO;
import com.dayan.content.dto.ContentInfoUpdateDTO;
import com.dayan.content.vo.ContentInfoVO;

/**
 * 内容信息服务。
 *
 * <p>{@code content_info} 为平台共享表（DayanTenantHandler 忽略），不参与渠道字段隔离。
 * 审核流（contentStatus）：0草稿 / 1待审 / 2通过 / 3拒绝 / 4下线。
 */
public interface ContentInfoService {

    /**
     * 分页查询。
     */
    PageResult<ContentInfoVO> page(ContentInfoQueryDTO query);

    /**
     * 详情。
     */
    ContentInfoVO getDetail(String contentCode);

    /**
     * 新增内容，返回生成的 contentCode（初始 contentStatus=0 草稿）。
     */
    String create(ContentInfoCreateDTO dto);

    /**
     * 修改内容。
     */
    void update(String contentCode, ContentInfoUpdateDTO dto);

    /**
     * 删除内容。
     */
    void delete(String contentCode);

    /**
     * 提交审核（草稿 0 → 待审 1）。
     */
    void submit(String contentCode);

    /**
     * 审核（待审 1 → 通过 2 / 拒绝 3）。
     */
    void audit(ContentInfoAuditDTO dto);

    /**
     * 发布（通过 2 → 正式上线，记录发布时间）。
     */
    void publish(String contentCode);

    /**
     * 下线（通过 2 → 下线 4）。
     */
    void offline(String contentCode);
}
