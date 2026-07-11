package com.mochao.module.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.ai.dto.AiGenerateDTO;
import com.mochao.module.ai.dto.AiRequestDTO;
import com.mochao.module.ai.entity.AiUsageLog;
import com.mochao.module.ai.service.AiService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/optimize")
    public Result<Map<String, Object>> optimize(@Valid @RequestBody AiRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.optimize(dto, userId));
    }

    @PostMapping("/expand")
    public Result<Map<String, Object>> expand(@Valid @RequestBody AiRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.expand(dto, userId));
    }

    @PostMapping("/condense")
    public Result<Map<String, Object>> condense(@Valid @RequestBody AiRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.condense(dto, userId));
    }

    @PostMapping("/continue")
    public Result<Map<String, Object>> continueWriting(@Valid @RequestBody AiRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.continueWriting(dto, userId));
    }

    @PostMapping("/polish-dialogue")
    public Result<Map<String, Object>> polishDialogue(@Valid @RequestBody AiRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.polishDialogue(dto, userId));
    }

    @PostMapping("/predict")
    public Result<Map<String, Object>> predict(@Valid @RequestBody AiRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.predict(dto, userId));
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@Valid @RequestBody AiGenerateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.generate(dto, userId));
    }

    @PostMapping("/adopt/{logId}")
    public Result<Void> adopt(@PathVariable Long logId) {
        Long userId = SecurityUtils.getCurrentUserId();
        aiService.adopt(logId, userId);
        return Result.success();
    }

    @GetMapping("/history")
    public Result<Page<AiUsageLog>> history(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.getHistory(userId, page, size));
    }
}
