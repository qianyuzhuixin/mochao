package com.mochao.module.ai.controller;

import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.ai.dto.AiConfigDTO;
import com.mochao.module.ai.entity.AiConfig;
import com.mochao.module.ai.service.AiConfigService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai-config")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    @GetMapping
    public Result<List<AiConfig>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiConfigService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public Result<AiConfig> get(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiConfigService.getById(id, userId));
    }

    @PostMapping
    public Result<AiConfig> create(@Valid @RequestBody AiConfigDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiConfigService.create(dto, userId));
    }

    @PutMapping("/{id}")
    public Result<AiConfig> update(@PathVariable Long id, @Valid @RequestBody AiConfigDTO dto) {
        dto.setId(id);
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiConfigService.update(dto, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        aiConfigService.delete(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        aiConfigService.activate(id, userId);
        return Result.success();
    }

    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection(@Valid @RequestBody AiConfigDTO dto) {
        return Result.success(aiConfigService.testConnection(dto));
    }
}
