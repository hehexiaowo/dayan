package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.entity.SystemMessageTemplate;
import com.dayan.system.mapper.SystemMessageTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 消息模板管理服务。
 *
 * <p>模板编码（template_code）全局唯一，且被 system_message.template_code 引用，
 * 创建后不可修改（更新时忽略该字段），避免发送记录失去关联。
 */
@Service
@RequiredArgsConstructor
public class SystemMessageTemplateService {

    private final SystemMessageTemplateMapper templateMapper;

    /**
     * 分页查询（编码/名称模糊、业务类型/渠道/状态精确）。
     */
    public PageResult<SystemMessageTemplate> page(long current, long size,
                                                  String templateCode, String templateName,
                                                  String bizType, Integer channelType, Integer status) {
        LambdaQueryWrapper<SystemMessageTemplate> wrapper = new LambdaQueryWrapper<SystemMessageTemplate>()
                .orderByAsc(SystemMessageTemplate::getSortOrder)
                .orderByAsc(SystemMessageTemplate::getId);
        if (templateCode != null && !templateCode.isEmpty()) {
            wrapper.like(SystemMessageTemplate::getTemplateCode, templateCode);
        }
        if (templateName != null && !templateName.isEmpty()) {
            wrapper.like(SystemMessageTemplate::getTemplateName, templateName);
        }
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(SystemMessageTemplate::getBizType, bizType);
        }
        if (channelType != null) {
            wrapper.eq(SystemMessageTemplate::getChannelType, channelType);
        }
        if (status != null) {
            wrapper.eq(SystemMessageTemplate::getStatus, status);
        }
        Page<SystemMessageTemplate> page = templateMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    public Long create(SystemMessageTemplate template) {
        validate(template);
        Long count = templateMapper.selectCount(new LambdaQueryWrapper<SystemMessageTemplate>()
                .eq(SystemMessageTemplate::getTemplateCode, template.getTemplateCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "模板编码已存在: " + template.getTemplateCode());
        }
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }
        templateMapper.insert(template);
        return template.getId();
    }

    public void update(Long id, SystemMessageTemplate template) {
        SystemMessageTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息模板不存在: id=" + id);
        }
        validate(template);
        // 模板编码被发送记录引用，创建后不可变更
        template.setId(id);
        template.setTemplateCode(existing.getTemplateCode());
        templateMapper.updateById(template);
    }

    public void delete(Long id) {
        SystemMessageTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息模板不存在: id=" + id);
        }
        templateMapper.deleteById(id);
    }

    private void validate(SystemMessageTemplate template) {
        if (template.getTemplateCode() == null || template.getTemplateCode().isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模板编码不能为空");
        }
        if (template.getTemplateName() == null || template.getTemplateName().isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模板名称不能为空");
        }
        if (template.getContent() == null || template.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模板正文不能为空");
        }
        if (template.getChannelType() == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "渠道类型不能为空");
        }
        // 站内信/推送/邮件类渠道必须携带标题
        boolean titleRequired = template.getChannelType() == 2 || template.getChannelType() == 3
                || template.getChannelType() == 6;
        if (titleRequired && (template.getTitle() == null || template.getTitle().isBlank())) {
            throw new BusinessException(ErrorCode.BUSINESS, "该渠道的消息标题为必填项");
        }
    }
}
