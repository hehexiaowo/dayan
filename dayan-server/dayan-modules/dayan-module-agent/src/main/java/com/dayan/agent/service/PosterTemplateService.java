package com.dayan.agent.service;

import com.dayan.agent.vo.PosterTemplateVO;

import java.util.List;

/**
 * 营销海报模板服务（代理人端只读浏览 + 公开分享）。
 */
public interface PosterTemplateService {

    /** 列出所有启用的海报模板（按 sortOrder 排序）。 */
    List<PosterTemplateVO> listActive(String categoryCode);

    /** 获取模板详情（按 templateCode）。 */
    PosterTemplateVO getDetail(String templateCode);
}
