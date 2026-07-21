package com.mochao.module.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.collection.dto.CollectionCreateDTO;
import com.mochao.module.collection.dto.CollectionQueryDTO;
import com.mochao.module.collection.dto.CollectionUpdateDTO;
import com.mochao.module.collection.entity.Collection;
import com.mochao.module.collection.entity.CollectionTag;
import com.mochao.module.collection.mapper.CollectionMapper;
import com.mochao.module.collection.mapper.CollectionTagMapper;
import com.mochao.module.collection.service.CollectionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final CollectionTagMapper collectionTagMapper;

    public CollectionServiceImpl(CollectionMapper collectionMapper, CollectionTagMapper collectionTagMapper) {
        this.collectionMapper = collectionMapper;
        this.collectionTagMapper = collectionTagMapper;
    }

    @Override
    public Collection createCollection(CollectionCreateDTO dto, Long userId) {
        Collection collection = new Collection();
        collection.setUserId(userId);
        collection.setBookId(dto.getBookId());
        collection.setContent(dto.getContent());
        collection.setType(dto.getType());
        collection.setContext(dto.getContext());
        collection.setNote(dto.getNote());
        collection.setTags(joinTags(dto.getTags()));
        collection.setSourceTitle(dto.getSourceTitle());
        collection.setSourceBook(dto.getSourceBook());
        collection.setSourceAuthor(dto.getSourceAuthor());
        collection.setCreatedAt(LocalDateTime.now());
        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.insert(collection);

        // 更新标签使用计数
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            updateTags(userId, dto.getTags());
        }

        return collection;
    }

    @Override
    public Page<Collection> getCollectionList(CollectionQueryDTO dto, Long userId) {
        Page<Collection> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId);

        if (StringUtils.hasText(dto.getType())) {
            wrapper.eq(Collection::getType, dto.getType());
        }
        if (dto.getBookId() != null) {
            wrapper.eq(Collection::getBookId, dto.getBookId());
        }
        if (StringUtils.hasText(dto.getTag())) {
            wrapper.like(Collection::getTags, dto.getTag());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(Collection::getContent, dto.getKeyword())
                    .or().like(Collection::getNote, dto.getKeyword())
                    .or().like(Collection::getSourceTitle, dto.getKeyword()));
        }
        wrapper.orderByDesc(Collection::getCreatedAt);
        return collectionMapper.selectPage(page, wrapper);
    }

    @Override
    public Collection getCollectionById(Long id, Long userId) {
        Collection collection = getOwnedCollection(id, userId);
        return collection;
    }

    @Override
    public Collection updateCollection(Long id, CollectionUpdateDTO dto, Long userId) {
        Collection collection = getOwnedCollection(id, userId);

        if (dto.getContent() != null) {
            collection.setContent(dto.getContent());
        }
        if (dto.getNote() != null) {
            collection.setNote(dto.getNote());
        }
        if (dto.getTags() != null) {
            collection.setTags(joinTags(dto.getTags()));
        }
        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.updateById(collection);
        return collection;
    }

    @Override
    public void deleteCollection(Long id, Long userId) {
        Collection collection = getOwnedCollection(id, userId);
        collectionMapper.deleteById(id);
    }

    @Override
    public List<CollectionTag> getUserTags(Long userId) {
        return collectionTagMapper.selectList(
                new LambdaQueryWrapper<CollectionTag>()
                        .eq(CollectionTag::getUserId, userId)
                        .orderByDesc(CollectionTag::getUseCount));
    }

    @Override
    public Collection getDailyCollection(Long userId) {
        long count = collectionMapper.selectCount(
                new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId));
        if (count == 0) {
            return null;
        }
        // 随机取一条
        int randomIndex = (int) (Math.random() * count);
        Page<Collection> page = new Page<>(randomIndex + 1, 1);
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId)
                .orderByDesc(Collection::getCreatedAt);
        Page<Collection> result = collectionMapper.selectPage(page, wrapper);
        return result.getRecords().isEmpty() ? null : result.getRecords().get(0);
    }

    @Override
    public String exportCollections(Long userId, String format) {
        List<Collection> list = collectionMapper.selectList(
                new LambdaQueryWrapper<Collection>()
                        .eq(Collection::getUserId, userId)
                        .orderByDesc(Collection::getCreatedAt));

        if (list.isEmpty()) {
            return "暂无收藏内容";
        }

        if ("markdown".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("# 我的收藏\n\n");
            for (Collection c : list) {
                sb.append("## ").append(c.getSourceTitle() != null ? c.getSourceTitle() : "未命名")
                        .append("\n\n");
                sb.append("> ").append(c.getContent()).append("\n\n");
                if (StringUtils.hasText(c.getNote())) {
                    sb.append("**笔记：** ").append(c.getNote()).append("\n\n");
                }
                if (StringUtils.hasText(c.getTags())) {
                    sb.append("**标签：** ").append(c.getTags()).append("\n\n");
                }
                sb.append("---\n\n");
            }
            return sb.toString();
        } else {
            // txt格式
            StringBuilder sb = new StringBuilder();
            sb.append("我的收藏\n").append("========\n\n");
            for (Collection c : list) {
                sb.append("【").append(c.getSourceTitle() != null ? c.getSourceTitle() : "未命名").append("】\n");
                sb.append(c.getContent()).append("\n");
                if (StringUtils.hasText(c.getNote())) {
                    sb.append("笔记：").append(c.getNote()).append("\n");
                }
                if (StringUtils.hasText(c.getTags())) {
                    sb.append("标签：").append(c.getTags()).append("\n");
                }
                sb.append("\n----------------------------------------\n\n");
            }
            return sb.toString();
        }
    }

    @Override
    public Map<String, Object> getCollectionStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        long total = collectionMapper.selectCount(
                new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId));
        stats.put("total", total);

        // 按类型统计
        List<Collection> all = collectionMapper.selectList(
                new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId));
        Map<String, Long> byType = all.stream()
                .filter(c -> StringUtils.hasText(c.getType()))
                .collect(Collectors.groupingBy(Collection::getType, Collectors.counting()));
        stats.put("byType", byType);

        // 标签统计
        long tagCount = collectionTagMapper.selectCount(
                new LambdaQueryWrapper<CollectionTag>().eq(CollectionTag::getUserId, userId));
        stats.put("tagCount", tagCount);

        return stats;
    }

    private Collection getOwnedCollection(Long id, Long userId) {
        Collection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收藏不存在");
        }
        if (!userId.equals(collection.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人收藏");
        }
        return collection;
    }

    private void updateTags(Long userId, List<String> tagList) {
        Set<String> uniqueTags = tagList.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        for (String tagName : uniqueTags) {
            CollectionTag existingTag = collectionTagMapper.selectOne(
                    new LambdaQueryWrapper<CollectionTag>()
                            .eq(CollectionTag::getUserId, userId)
                            .eq(CollectionTag::getName, tagName));
            if (existingTag == null) {
                CollectionTag newTag = new CollectionTag();
                newTag.setUserId(userId);
                newTag.setName(tagName);
                newTag.setUseCount(1);
                newTag.setCreatedAt(LocalDateTime.now());
                collectionTagMapper.insert(newTag);
            } else {
                existingTag.setUseCount(existingTag.getUseCount() + 1);
                collectionTagMapper.updateById(existingTag);
            }
        }
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }
}
