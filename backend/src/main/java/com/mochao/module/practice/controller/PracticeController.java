package com.mochao.module.practice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.practice.dto.PracticeCompleteDTO;
import com.mochao.module.practice.dto.PracticeProgressDTO;
import com.mochao.module.practice.dto.PracticeStartDTO;
import com.mochao.module.practice.entity.PracticeSession;
import com.mochao.module.practice.service.PracticeService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/start")
    public Result<PracticeSession> start(@Valid @RequestBody PracticeStartDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.startPractice(dto, userId));
    }

    @PutMapping("/{id}/progress")
    public Result<PracticeSession> updateProgress(@PathVariable Long id,
                                                   @RequestBody PracticeProgressDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.updateProgress(id, dto, userId));
    }

    @PostMapping("/{id}/pause")
    public Result<PracticeSession> pause(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.pausePractice(id, userId));
    }

    @PostMapping("/{id}/resume")
    public Result<PracticeSession> resume(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.resumePractice(id, userId));
    }

    @PostMapping("/{id}/complete")
    public Result<PracticeSession> complete(@PathVariable Long id,
                                             @RequestBody PracticeCompleteDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.completePractice(id, dto, userId));
    }

    @GetMapping("/active")
    public Result<PracticeSession> active() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.getActivePractice(userId));
    }

    @GetMapping("/history")
    public Result<Page<PracticeSession>> history(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(practiceService.getPracticeHistory(userId, page, size));
    }
}
