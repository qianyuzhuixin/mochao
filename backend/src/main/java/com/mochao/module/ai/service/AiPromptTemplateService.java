package com.mochao.module.ai.service;

import com.mochao.module.ai.dto.AiPromptTemplateDTO;
import com.mochao.module.ai.entity.AiPromptTemplate;

import java.util.List;

public interface AiPromptTemplateService {

    /** 获取用户某功能的提示词模板（优先用户自定义，fallback系统默认） */
    AiPromptTemplate getTemplate(Long userId, String feature);

    /** 获取用户所有模板 */
    List<AiPromptTemplate> getUserTemplates(Long userId);

    /** 创建/更新模板 */
    AiPromptTemplate saveOrUpdate(Long userId, AiPromptTemplateDTO dto);

    /** 重置为系统默认 */
    void resetToDefault(Long userId, String feature);
}
