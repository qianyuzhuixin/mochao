package com.mochao.module.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.ai.dto.AiGenerateDTO;
import com.mochao.module.ai.dto.AiRequestDTO;
import com.mochao.module.ai.entity.AiUsageLog;

import java.util.Map;

public interface AiService {

    Map<String, Object> optimize(AiRequestDTO dto, Long userId);

    Map<String, Object> expand(AiRequestDTO dto, Long userId);

    Map<String, Object> condense(AiRequestDTO dto, Long userId);

    Map<String, Object> continueWriting(AiRequestDTO dto, Long userId);

    Map<String, Object> polishDialogue(AiRequestDTO dto, Long userId);

    Map<String, Object> predict(AiRequestDTO dto, Long userId);

    Map<String, Object> generate(AiGenerateDTO dto, Long userId);

    void adopt(Long logId, Long userId);

    Page<AiUsageLog> getHistory(Long userId, Integer page, Integer size);
}
