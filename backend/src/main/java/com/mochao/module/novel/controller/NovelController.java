package com.mochao.module.novel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.novel.dto.*;
import com.mochao.module.novel.entity.*;
import com.mochao.module.novel.service.NovelService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/novels")
public class NovelController {

    private final NovelService novelService;

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    // ==================== Novel CRUD ====================

    @GetMapping
    public Result<Page<Novel>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getNovelList(userId, page, size, status));
    }

    @GetMapping("/{id}")
    public Result<Novel> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getNovelById(id, userId));
    }

    @PostMapping
    public Result<Novel> create(@Valid @RequestBody NovelCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.createNovel(dto, userId));
    }

    @PutMapping("/{id}")
    public Result<Novel> update(@PathVariable Long id, @RequestBody NovelUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateNovel(id, dto, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        novelService.deleteNovel(id, userId);
        return Result.success();
    }

    // ==================== Progress ====================

    @GetMapping("/{id}/progress")
    public Result<Map<String, Object>> progress(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getNovelProgress(id, userId));
    }

    // ==================== Outline ====================

    @GetMapping("/{id}/outline")
    public Result<NovelOutline> getOutline(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getOutline(id, userId));
    }

    @PutMapping("/{id}/outline")
    public Result<NovelOutline> updateOutline(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateOutline(id, body.get("content"), userId));
    }

    // ==================== Worldview ====================

    @GetMapping("/{id}/worldview")
    public Result<NovelWorldview> getWorldview(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getWorldview(id, userId));
    }

    @PutMapping("/{id}/worldview")
    public Result<NovelWorldview> updateWorldview(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateWorldview(id, body.get("content"), userId));
    }

    // ==================== Characters ====================

    @GetMapping("/{id}/characters")
    public Result<List<NovelCharacter>> getCharacters(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getCharacters(id, userId));
    }

    @PostMapping("/{id}/characters")
    public Result<NovelCharacter> createCharacter(@PathVariable Long id,
                                                   @Valid @RequestBody NovelCharacterDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.createCharacter(id, dto, userId));
    }

    @PutMapping("/characters/{characterId}")
    public Result<NovelCharacter> updateCharacter(@PathVariable Long characterId,
                                                   @RequestBody NovelCharacterDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateCharacter(characterId, dto, userId));
    }

    @DeleteMapping("/characters/{characterId}")
    public Result<Void> deleteCharacter(@PathVariable Long characterId) {
        Long userId = SecurityUtils.getCurrentUserId();
        novelService.deleteCharacter(characterId, userId);
        return Result.success();
    }

    // ==================== Items ====================

    @GetMapping("/{id}/items")
    public Result<List<NovelItem>> getItems(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getItems(id, userId));
    }

    @PostMapping("/{id}/items")
    public Result<NovelItem> createItem(@PathVariable Long id, @Valid @RequestBody NovelItemDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.createItem(id, dto, userId));
    }

    @PutMapping("/items/{itemId}")
    public Result<NovelItem> updateItem(@PathVariable Long itemId, @RequestBody NovelItemDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateItem(itemId, dto, userId));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        Long userId = SecurityUtils.getCurrentUserId();
        novelService.deleteItem(itemId, userId);
        return Result.success();
    }

    // ==================== Chapter Outlines ====================

    @GetMapping("/{id}/chapter-outlines")
    public Result<List<NovelChapterOutline>> getChapterOutlines(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getChapterOutlines(id, userId));
    }

    @PostMapping("/{id}/chapter-outlines")
    public Result<NovelChapterOutline> createChapterOutline(@PathVariable Long id,
                                                             @Valid @RequestBody NovelChapterOutlineDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.createChapterOutline(id, dto, userId));
    }

    @PutMapping("/chapter-outlines/{outlineId}")
    public Result<NovelChapterOutline> updateChapterOutline(@PathVariable Long outlineId,
                                                             @RequestBody NovelChapterOutlineDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateChapterOutline(outlineId, dto, userId));
    }

    @DeleteMapping("/chapter-outlines/{outlineId}")
    public Result<Void> deleteChapterOutline(@PathVariable Long outlineId) {
        Long userId = SecurityUtils.getCurrentUserId();
        novelService.deleteChapterOutline(outlineId, userId);
        return Result.success();
    }

    // ==================== Chapters ====================

    @GetMapping("/{id}/chapters")
    public Result<Page<NovelChapter>> getChapters(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getChapters(id, userId, page, size));
    }

    @GetMapping("/chapters/{chapterId}")
    public Result<NovelChapter> getChapter(@PathVariable Long chapterId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.getChapter(chapterId, userId));
    }

    @PostMapping("/{id}/chapters")
    public Result<NovelChapter> createChapter(@PathVariable Long id, @Valid @RequestBody NovelChapterDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.createChapter(id, dto, userId));
    }

    @PutMapping("/chapters/{chapterId}")
    public Result<NovelChapter> updateChapter(@PathVariable Long chapterId, @RequestBody NovelChapterDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(novelService.updateChapter(chapterId, dto, userId));
    }

    @DeleteMapping("/chapters/{chapterId}")
    public Result<Void> deleteChapter(@PathVariable Long chapterId) {
        Long userId = SecurityUtils.getCurrentUserId();
        novelService.deleteChapter(chapterId, userId);
        return Result.success();
    }
}
