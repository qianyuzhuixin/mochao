package com.mochao.module.ai.controller;

import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.ai.dto.AiPromptTemplateDTO;
import com.mochao.module.ai.entity.AiPromptTemplate;
import com.mochao.module.ai.service.AiPromptTemplateService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/ai/prompt-templates")
public class AiPromptTemplateController {

    private final AiPromptTemplateService service;

    public AiPromptTemplateController(AiPromptTemplateService service) {
        this.service = service;
    }

    /** 获取用户所有模板 */
    @GetMapping
    public Result<List<AiPromptTemplate>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.getUserTemplates(userId));
    }

    /** 创建/更新模板 */
    @PutMapping
    public Result<AiPromptTemplate> saveOrUpdate(@Valid @RequestBody AiPromptTemplateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.saveOrUpdate(userId, dto));
    }

    /** 重置为系统默认 */
    @DeleteMapping("/{feature}")
    public Result<Void> resetToDefault(@PathVariable String feature) {
        Long userId = SecurityUtils.getCurrentUserId();
        service.resetToDefault(userId, feature);
        return Result.success();
    }
}
