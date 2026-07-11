package com.mochao.module.ai.service;

import com.mochao.module.ai.dto.AiConfigDTO;
import com.mochao.module.ai.entity.AiConfig;

import java.util.List;
import java.util.Map;

public interface AiConfigService {

    List<AiConfig> listByUser(Long userId);

    AiConfig getById(Long id, Long userId);

    AiConfig create(AiConfigDTO dto, Long userId);

    AiConfig update(AiConfigDTO dto, Long userId);

    void delete(Long id, Long userId);

    void activate(Long id, Long userId);

    Map<String, Object> testConnection(AiConfigDTO dto);

    AiConfig getActiveConfig(Long userId);
}
