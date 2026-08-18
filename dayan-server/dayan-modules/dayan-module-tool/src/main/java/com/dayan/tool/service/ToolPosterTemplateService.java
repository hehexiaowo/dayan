package com.dayan.tool.service;

import com.dayan.tool.vo.ToolPosterTemplateVO;

import java.util.List;

/**
 * 营销海报模板服务（代理人端只读浏览 + 公开分享）。
 */
public interface ToolPosterTemplateService {

    /** 列出所有启用的海报模板（按 sortOrder 排序）。 */
    List<ToolPosterTemplateVO> listActive(String categoryCode);

    /** 获取模板详情（按 templateCode）。 */
    ToolPosterTemplateVO getDetail(String templateCode);
}
