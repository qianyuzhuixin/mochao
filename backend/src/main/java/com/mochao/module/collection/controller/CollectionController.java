package com.mochao.module.collection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.collection.dto.CollectionCreateDTO;
import com.mochao.module.collection.dto.CollectionQueryDTO;
import com.mochao.module.collection.dto.CollectionUpdateDTO;
import com.mochao.module.collection.entity.Collection;
import com.mochao.module.collection.entity.CollectionTag;
import com.mochao.module.collection.service.CollectionService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping
    public Result<Collection> create(@Valid @RequestBody CollectionCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.createCollection(dto, userId));
    }

    @GetMapping
    public Result<Page<Collection>> list(CollectionQueryDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.getCollectionList(dto, userId));
    }

    @GetMapping("/{id}")
    public Result<Collection> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.getCollectionById(id, userId));
    }

    @PutMapping("/{id}")
    public Result<Collection> update(@PathVariable Long id, @RequestBody CollectionUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.updateCollection(id, dto, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        collectionService.deleteCollection(id, userId);
        return Result.success();
    }

    @GetMapping("/tags")
    public Result<List<CollectionTag>> tags() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.getUserTags(userId));
    }

    @GetMapping("/daily")
    public Result<Collection> daily() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.getDailyCollection(userId));
    }

    @GetMapping("/export")
    public Result<String> export(@RequestParam(defaultValue = "txt") String format) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.exportCollections(userId, format));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(collectionService.getCollectionStats(userId));
    }
}
