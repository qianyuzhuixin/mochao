package com.mochao.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.ai.dto.AiPromptTemplateDTO;
import com.mochao.module.ai.entity.AiPromptTemplate;
import com.mochao.module.ai.mapper.AiPromptTemplateMapper;
import com.mochao.module.ai.service.AiPromptTemplateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiPromptTemplateServiceImpl implements AiPromptTemplateService {

    private static final Long SYSTEM_USER_ID = 0L;

    private final AiPromptTemplateMapper mapper;

    public AiPromptTemplateServiceImpl(AiPromptTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AiPromptTemplate getTemplate(Long userId, String feature) {
        // 优先查用户自定义模板
        AiPromptTemplate tpl = mapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getUserId, userId)
                        .eq(AiPromptTemplate::getFeature, feature)
                        .eq(AiPromptTemplate::getIsActive, true));
        if (tpl != null) return tpl;

        // fallback: 系统默认模板
        tpl = mapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getUserId, SYSTEM_USER_ID)
                        .eq(AiPromptTemplate::getFeature, feature)
                        .eq(AiPromptTemplate::getIsActive, true));
        if (tpl != null) return tpl;

        // 终极兜底：返回 null，调用方自行处理
        return null;
    }

    @Override
    public List<AiPromptTemplate> getUserTemplates(Long userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getUserId, userId)
                        .orderByAsc(AiPromptTemplate::getFeature));
    }

    @Override
    public AiPromptTemplate saveOrUpdate(Long userId, AiPromptTemplateDTO dto) {
        AiPromptTemplate existing = mapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getUserId, userId)
                        .eq(AiPromptTemplate::getFeature, dto.getFeature()));

        if (existing != null) {
            if (dto.getName() != null) existing.setName(dto.getName());
            existing.setSystemPrompt(dto.getSystemPrompt());
            if (dto.getIsActive() != null) existing.setIsActive(dto.getIsActive());
            existing.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(existing);
            return existing;
        }

        AiPromptTemplate tpl = new AiPromptTemplate();
        tpl.setUserId(userId);
        tpl.setFeature(dto.getFeature());
        tpl.setName(dto.getName() != null ? dto.getName() : dto.getFeature());
        tpl.setSystemPrompt(dto.getSystemPrompt());
        tpl.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        tpl.setCreatedAt(LocalDateTime.now());
        tpl.setUpdatedAt(LocalDateTime.now());
        mapper.insert(tpl);
        return tpl;
    }

    @Override
    public void resetToDefault(Long userId, String feature) {
        mapper.delete(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getUserId, userId)
                .eq(AiPromptTemplate::getFeature, feature));
    }
}
