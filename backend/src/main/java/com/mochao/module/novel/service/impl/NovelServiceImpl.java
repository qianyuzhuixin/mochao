package com.mochao.module.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.constant.Constants;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.novel.dto.*;
import com.mochao.module.novel.entity.*;
import com.mochao.module.novel.mapper.*;
import com.mochao.module.novel.service.NovelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NovelServiceImpl implements NovelService {

    private final NovelMapper novelMapper;
    private final NovelOutlineMapper novelOutlineMapper;
    private final NovelWorldviewMapper novelWorldviewMapper;
    private final NovelCharacterMapper novelCharacterMapper;
    private final NovelItemMapper novelItemMapper;
    private final NovelChapterOutlineMapper novelChapterOutlineMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final NovelDailyProgressMapper novelDailyProgressMapper;
    private final NovelVolumeMapper novelVolumeMapper;
    private final NovelActMapper novelActMapper;

    public NovelServiceImpl(NovelMapper novelMapper,
                            NovelOutlineMapper novelOutlineMapper,
                            NovelWorldviewMapper novelWorldviewMapper,
                            NovelCharacterMapper novelCharacterMapper,
                            NovelItemMapper novelItemMapper,
                            NovelChapterOutlineMapper novelChapterOutlineMapper,
                            NovelChapterMapper novelChapterMapper,
                            NovelDailyProgressMapper novelDailyProgressMapper,
                            NovelVolumeMapper novelVolumeMapper,
                            NovelActMapper novelActMapper) {
        this.novelMapper = novelMapper;
        this.novelOutlineMapper = novelOutlineMapper;
        this.novelWorldviewMapper = novelWorldviewMapper;
        this.novelCharacterMapper = novelCharacterMapper;
        this.novelItemMapper = novelItemMapper;
        this.novelChapterOutlineMapper = novelChapterOutlineMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.novelDailyProgressMapper = novelDailyProgressMapper;
        this.novelVolumeMapper = novelVolumeMapper;
        this.novelActMapper = novelActMapper;
    }

    // ==================== Novel CRUD ====================

    @Override
    public Page<Novel> getNovelList(Long userId, Integer page, Integer size, String status) {
        Page<Novel> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Novel::getUserId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Novel::getStatus, status);
        }
        wrapper.orderByDesc(Novel::getUpdatedAt);
        return novelMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Novel getNovelById(Long id, Long userId) {
        return getOwnedNovel(id, userId);
    }

    @Override
    public Novel createNovel(NovelCreateDTO dto, Long userId) {
        Novel novel = new Novel();
        novel.setUserId(userId);
        novel.setTitle(dto.getTitle());
        novel.setGenre(dto.getGenre());
        novel.setSummary(dto.getSummary());
        novel.setCover(dto.getCover());
        novel.setStatus(Constants.NOVEL_STATUS_DRAFT);
        novel.setTargetWords(dto.getTargetWords() != null ? dto.getTargetWords() : 0);
        novel.setTotalWords(0);
        novel.setChapterCount(0);
        novel.setCompletedChapters(0);
        novel.setCreatedAt(LocalDateTime.now());
        novel.setUpdatedAt(LocalDateTime.now());
        novelMapper.insert(novel);
        return novel;
    }

    @Override
    public Novel updateNovel(Long id, NovelUpdateDTO dto, Long userId) {
        Novel novel = getOwnedNovel(id, userId);
        if (dto.getTitle() != null) novel.setTitle(dto.getTitle());
        if (dto.getGenre() != null) novel.setGenre(dto.getGenre());
        if (dto.getSummary() != null) novel.setSummary(dto.getSummary());
        if (dto.getCover() != null) novel.setCover(dto.getCover());
        if (dto.getStatus() != null) novel.setStatus(dto.getStatus());
        if (dto.getTargetWords() != null) novel.setTargetWords(dto.getTargetWords());
        novel.setUpdatedAt(LocalDateTime.now());
        novelMapper.updateById(novel);
        return novel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNovel(Long id, Long userId) {
        Novel novel = getOwnedNovel(id, userId);
        // 删除关联数据
        novelOutlineMapper.delete(new LambdaQueryWrapper<NovelOutline>().eq(NovelOutline::getNovelId, id));
        novelWorldviewMapper.delete(new LambdaQueryWrapper<NovelWorldview>().eq(NovelWorldview::getNovelId, id));
        novelCharacterMapper.delete(new LambdaQueryWrapper<NovelCharacter>().eq(NovelCharacter::getNovelId, id));
        novelItemMapper.delete(new LambdaQueryWrapper<NovelItem>().eq(NovelItem::getNovelId, id));
        novelChapterOutlineMapper.delete(new LambdaQueryWrapper<NovelChapterOutline>().eq(NovelChapterOutline::getNovelId, id));
        novelChapterMapper.delete(new LambdaQueryWrapper<NovelChapter>().eq(NovelChapter::getNovelId, id));
        novelDailyProgressMapper.delete(new LambdaQueryWrapper<NovelDailyProgress>().eq(NovelDailyProgress::getNovelId, id));
        novelActMapper.delete(new LambdaQueryWrapper<NovelAct>().eq(NovelAct::getNovelId, id));
        novelVolumeMapper.delete(new LambdaQueryWrapper<NovelVolume>().eq(NovelVolume::getNovelId, id));
        novelMapper.deleteById(id);
    }

    // ==================== Progress ====================

    @Override
    public Map<String, Object> getNovelProgress(Long id, Long userId) {
        Novel novel = getOwnedNovel(id, userId);
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalWords", novel.getTotalWords());
        progress.put("targetWords", novel.getTargetWords());
        progress.put("chapterCount", novel.getChapterCount());
        progress.put("completedChapters", novel.getCompletedChapters());
        double completionRate = novel.getTargetWords() > 0
                ? (double) novel.getTotalWords() / novel.getTargetWords() * 100 : 0;
        progress.put("completionRate", Math.round(completionRate * 100) / 100.0);

        // 今日进度
        LocalDate today = LocalDate.now();
        NovelDailyProgress todayProgress = novelDailyProgressMapper.selectOne(
                new LambdaQueryWrapper<NovelDailyProgress>()
                        .eq(NovelDailyProgress::getNovelId, id)
                        .eq(NovelDailyProgress::getProgressDate, today));
        progress.put("todayWords", todayProgress != null ? todayProgress.getWordsWritten() : 0);

        return progress;
    }

    // ==================== Outline ====================

    @Override
    public NovelOutline getOutline(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelOutline outline = novelOutlineMapper.selectOne(
                new LambdaQueryWrapper<NovelOutline>().eq(NovelOutline::getNovelId, novelId));
        return outline;
    }

    @Override
    public NovelOutline updateOutline(Long novelId, String content, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelOutline outline = novelOutlineMapper.selectOne(
                new LambdaQueryWrapper<NovelOutline>().eq(NovelOutline::getNovelId, novelId));
        if (outline == null) {
            outline = new NovelOutline();
            outline.setNovelId(novelId);
            outline.setContent(content);
            outline.setCreatedAt(LocalDateTime.now());
            outline.setUpdatedAt(LocalDateTime.now());
            novelOutlineMapper.insert(outline);
        } else {
            outline.setContent(content);
            outline.setUpdatedAt(LocalDateTime.now());
            novelOutlineMapper.updateById(outline);
        }
        return outline;
    }

    // ==================== Worldview ====================

    @Override
    public NovelWorldview getWorldview(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        return novelWorldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>().eq(NovelWorldview::getNovelId, novelId));
    }

    @Override
    public NovelWorldview updateWorldview(Long novelId, String content, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelWorldview worldview = novelWorldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>().eq(NovelWorldview::getNovelId, novelId));
        if (worldview == null) {
            worldview = new NovelWorldview();
            worldview.setNovelId(novelId);
            worldview.setContent(content);
            worldview.setCreatedAt(LocalDateTime.now());
            worldview.setUpdatedAt(LocalDateTime.now());
            novelWorldviewMapper.insert(worldview);
        } else {
            worldview.setContent(content);
            worldview.setUpdatedAt(LocalDateTime.now());
            novelWorldviewMapper.updateById(worldview);
        }
        return worldview;
    }

    // ==================== Characters ====================

    @Override
    public List<NovelCharacter> getCharacters(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        return novelCharacterMapper.selectList(
                new LambdaQueryWrapper<NovelCharacter>()
                        .eq(NovelCharacter::getNovelId, novelId)
                        .orderByAsc(NovelCharacter::getSortOrder));
    }

    @Override
    public NovelCharacter createCharacter(Long novelId, NovelCharacterDTO dto, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelCharacter character = new NovelCharacter();
        character.setNovelId(novelId);
        character.setName(dto.getName());
        character.setRole(dto.getRole());
        character.setAvatar(dto.getAvatar());
        character.setAppearance(dto.getAppearance());
        character.setPersonality(dto.getPersonality());
        character.setBackground(dto.getBackground());
        character.setRelationships(dto.getRelationships());
        character.setFirstAppearance(dto.getFirstAppearance());
        character.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        character.setCreatedAt(LocalDateTime.now());
        character.setUpdatedAt(LocalDateTime.now());
        novelCharacterMapper.insert(character);
        return character;
    }

    @Override
    public NovelCharacter updateCharacter(Long characterId, NovelCharacterDTO dto, Long userId) {
        NovelCharacter character = novelCharacterMapper.selectById(characterId);
        if (character == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "人物不存在");
        }
        getOwnedNovel(character.getNovelId(), userId);
        if (dto.getName() != null) character.setName(dto.getName());
        if (dto.getRole() != null) character.setRole(dto.getRole());
        if (dto.getAvatar() != null) character.setAvatar(dto.getAvatar());
        if (dto.getAppearance() != null) character.setAppearance(dto.getAppearance());
        if (dto.getPersonality() != null) character.setPersonality(dto.getPersonality());
        if (dto.getBackground() != null) character.setBackground(dto.getBackground());
        if (dto.getRelationships() != null) character.setRelationships(dto.getRelationships());
        if (dto.getFirstAppearance() != null) character.setFirstAppearance(dto.getFirstAppearance());
        if (dto.getSortOrder() != null) character.setSortOrder(dto.getSortOrder());
        character.setUpdatedAt(LocalDateTime.now());
        novelCharacterMapper.updateById(character);
        return character;
    }

    @Override
    public void deleteCharacter(Long characterId, Long userId) {
        NovelCharacter character = novelCharacterMapper.selectById(characterId);
        if (character == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "人物不存在");
        }
        getOwnedNovel(character.getNovelId(), userId);
        novelCharacterMapper.deleteById(characterId);
    }

    // ==================== Items ====================

    @Override
    public List<NovelItem> getItems(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        return novelItemMapper.selectList(
                new LambdaQueryWrapper<NovelItem>()
                        .eq(NovelItem::getNovelId, novelId)
                        .orderByAsc(NovelItem::getSortOrder));
    }

    @Override
    public NovelItem createItem(Long novelId, NovelItemDTO dto, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelItem item = new NovelItem();
        item.setNovelId(novelId);
        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setAppearance(dto.getAppearance());
        item.setOrigin(dto.getOrigin());
        item.setAttributes(dto.getAttributes());
        item.setOwner(dto.getOwner());
        item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        novelItemMapper.insert(item);
        return item;
    }

    @Override
    public NovelItem updateItem(Long itemId, NovelItemDTO dto, Long userId) {
        NovelItem item = novelItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物品不存在");
        }
        getOwnedNovel(item.getNovelId(), userId);
        if (dto.getName() != null) item.setName(dto.getName());
        if (dto.getCategory() != null) item.setCategory(dto.getCategory());
        if (dto.getAppearance() != null) item.setAppearance(dto.getAppearance());
        if (dto.getOrigin() != null) item.setOrigin(dto.getOrigin());
        if (dto.getAttributes() != null) item.setAttributes(dto.getAttributes());
        if (dto.getOwner() != null) item.setOwner(dto.getOwner());
        if (dto.getSortOrder() != null) item.setSortOrder(dto.getSortOrder());
        item.setUpdatedAt(LocalDateTime.now());
        novelItemMapper.updateById(item);
        return item;
    }

    @Override
    public void deleteItem(Long itemId, Long userId) {
        NovelItem item = novelItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物品不存在");
        }
        getOwnedNovel(item.getNovelId(), userId);
        novelItemMapper.deleteById(itemId);
    }

    // ==================== Chapter Outlines ====================

    @Override
    public List<NovelChapterOutline> getChapterOutlines(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        return novelChapterOutlineMapper.selectList(
                new LambdaQueryWrapper<NovelChapterOutline>()
                        .eq(NovelChapterOutline::getNovelId, novelId)
                        .orderByAsc(NovelChapterOutline::getChapterNumber));
    }

    @Override
    public NovelChapterOutline createChapterOutline(Long novelId, NovelChapterOutlineDTO dto, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelChapterOutline outline = new NovelChapterOutline();
        outline.setNovelId(novelId);
        outline.setActId(dto.getActId());
        outline.setChapterNumber(dto.getChapterNumber());
        outline.setTitle(dto.getTitle());
        outline.setSummary(dto.getSummary());
        outline.setDetail(dto.getDetail());
        outline.setStatus(Constants.CHAPTER_STATUS_DRAFT);
        outline.setCreatedAt(LocalDateTime.now());
        outline.setUpdatedAt(LocalDateTime.now());
        novelChapterOutlineMapper.insert(outline);
        return outline;
    }

    @Override
    public NovelChapterOutline updateChapterOutline(Long outlineId, NovelChapterOutlineDTO dto, Long userId) {
        NovelChapterOutline outline = novelChapterOutlineMapper.selectById(outlineId);
        if (outline == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章纲不存在");
        }
        getOwnedNovel(outline.getNovelId(), userId);
        if (dto.getActId() != null) outline.setActId(dto.getActId());
        if (dto.getChapterNumber() != null) outline.setChapterNumber(dto.getChapterNumber());
        if (dto.getTitle() != null) outline.setTitle(dto.getTitle());
        if (dto.getSummary() != null) outline.setSummary(dto.getSummary());
        if (dto.getDetail() != null) outline.setDetail(dto.getDetail());
        if (dto.getStatus() != null) outline.setStatus(dto.getStatus());
        outline.setUpdatedAt(LocalDateTime.now());
        novelChapterOutlineMapper.updateById(outline);
        return outline;
    }

    @Override
    public void deleteChapterOutline(Long outlineId, Long userId) {
        NovelChapterOutline outline = novelChapterOutlineMapper.selectById(outlineId);
        if (outline == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章纲不存在");
        }
        getOwnedNovel(outline.getNovelId(), userId);
        novelChapterOutlineMapper.deleteById(outlineId);
    }

    // ==================== Chapters ====================

    @Override
    public Page<NovelChapter> getChapters(Long novelId, Long userId, Integer page, Integer size) {
        getOwnedNovel(novelId, userId);
        Page<NovelChapter> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelChapter::getNovelId, novelId)
                .orderByAsc(NovelChapter::getChapterNumber);
        return novelChapterMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public NovelChapter getChapter(Long chapterId, Long userId) {
        NovelChapter chapter = novelChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章节不存在");
        }
        getOwnedNovel(chapter.getNovelId(), userId);
        return chapter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelChapter createChapter(Long novelId, NovelChapterDTO dto, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelChapter chapter = new NovelChapter();
        chapter.setNovelId(novelId);
        chapter.setOutlineId(dto.getOutlineId());
        chapter.setChapterNumber(dto.getChapterNumber());
        chapter.setTitle(dto.getTitle());
        chapter.setContent(dto.getContent());
        // 字数统计：去除空白字符后统计（中英文混排更准确）
        String content = dto.getContent();
        int wordCount = content != null ? content.replaceAll("\\s+", "").length() : 0;
        chapter.setWordCount(wordCount);
        chapter.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.CHAPTER_STATUS_DRAFT);
        chapter.setCreatedAt(LocalDateTime.now());
        chapter.setUpdatedAt(LocalDateTime.now());
        novelChapterMapper.insert(chapter);

        // 原子更新小说统计（解决并发竞态条件）
        LambdaUpdateWrapper<Novel> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Novel::getId, novelId)
                .setSql("chapter_count = chapter_count + 1")
                .setSql("total_words = total_words + " + wordCount);
        if (Constants.CHAPTER_STATUS_PUBLISHED.equals(chapter.getStatus())) {
            updateWrapper.setSql("completed_chapters = completed_chapters + 1");
        }
        updateWrapper.set(Novel::getUpdatedAt, LocalDateTime.now());
        novelMapper.update(null, updateWrapper);

        // 更新每日进度
        updateDailyProgress(novelId, userId, wordCount);

        return chapter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelChapter updateChapter(Long chapterId, NovelChapterDTO dto, Long userId) {
        NovelChapter chapter = novelChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章节不存在");
        }
        getOwnedNovel(chapter.getNovelId(), userId);

        int oldWordCount = chapter.getWordCount() != null ? chapter.getWordCount() : 0;
        boolean wasPublished = Constants.CHAPTER_STATUS_PUBLISHED.equals(chapter.getStatus());

        if (dto.getOutlineId() != null) chapter.setOutlineId(dto.getOutlineId());
        if (dto.getChapterNumber() != null) chapter.setChapterNumber(dto.getChapterNumber());
        if (dto.getTitle() != null) chapter.setTitle(dto.getTitle());
        if (dto.getContent() != null) {
            chapter.setContent(dto.getContent());
            chapter.setWordCount(dto.getContent().replaceAll("\\s+", "").length());
        }
        if (dto.getStatus() != null) chapter.setStatus(dto.getStatus());
        chapter.setUpdatedAt(LocalDateTime.now());
        novelChapterMapper.updateById(chapter);

        // 原子更新小说统计（解决并发竞态条件）
        int newWordCount = chapter.getWordCount() != null ? chapter.getWordCount() : 0;
        int wordDelta = newWordCount - oldWordCount;
        boolean nowPublished = Constants.CHAPTER_STATUS_PUBLISHED.equals(chapter.getStatus());

        LambdaUpdateWrapper<Novel> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Novel::getId, chapter.getNovelId())
                .setSql("total_words = total_words + " + wordDelta);
        if (nowPublished && !wasPublished) {
            updateWrapper.setSql("completed_chapters = completed_chapters + 1");
        } else if (!nowPublished && wasPublished) {
            updateWrapper.setSql("completed_chapters = completed_chapters - 1");
        }
        updateWrapper.set(Novel::getUpdatedAt, LocalDateTime.now());
        novelMapper.update(null, updateWrapper);

        return chapter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapter(Long chapterId, Long userId) {
        NovelChapter chapter = novelChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章节不存在");
        }
        getOwnedNovel(chapter.getNovelId(), userId);

        int wordCount = chapter.getWordCount() != null ? chapter.getWordCount() : 0;
        boolean wasPublished = Constants.CHAPTER_STATUS_PUBLISHED.equals(chapter.getStatus());

        // 原子更新小说统计（解决并发竞态条件）
        LambdaUpdateWrapper<Novel> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Novel::getId, chapter.getNovelId())
                .setSql("chapter_count = chapter_count - 1")
                .setSql("total_words = total_words - " + wordCount);
        if (wasPublished) {
            updateWrapper.setSql("completed_chapters = completed_chapters - 1");
        }
        updateWrapper.set(Novel::getUpdatedAt, LocalDateTime.now());
        novelMapper.update(null, updateWrapper);

        novelChapterMapper.deleteById(chapterId);
    }

    // ==================== Volume Outlines ====================

    @Override
    public List<NovelVolume> getVolumes(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        return novelVolumeMapper.selectList(
                new LambdaQueryWrapper<NovelVolume>()
                        .eq(NovelVolume::getNovelId, novelId)
                        .orderByAsc(NovelVolume::getSortOrder));
    }

    @Override
    public NovelVolume createVolume(Long novelId, NovelVolumeDTO dto, Long userId) {
        getOwnedNovel(novelId, userId);
        NovelVolume volume = new NovelVolume();
        volume.setNovelId(novelId);
        volume.setVolumeNumber(dto.getVolumeNumber() != null ? dto.getVolumeNumber() : getNextVolumeNumber(novelId));
        volume.setTitle(dto.getTitle());
        volume.setOutline(dto.getOutline());
        volume.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : volume.getVolumeNumber());
        volume.setCreatedAt(LocalDateTime.now());
        volume.setUpdatedAt(LocalDateTime.now());
        novelVolumeMapper.insert(volume);
        return volume;
    }

    @Override
    public NovelVolume updateVolume(Long volumeId, NovelVolumeDTO dto, Long userId) {
        NovelVolume volume = novelVolumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "卷不存在");
        }
        getOwnedNovel(volume.getNovelId(), userId);
        if (dto.getVolumeNumber() != null) volume.setVolumeNumber(dto.getVolumeNumber());
        if (dto.getTitle() != null) volume.setTitle(dto.getTitle());
        if (dto.getOutline() != null) volume.setOutline(dto.getOutline());
        if (dto.getSortOrder() != null) volume.setSortOrder(dto.getSortOrder());
        volume.setUpdatedAt(LocalDateTime.now());
        novelVolumeMapper.updateById(volume);
        return volume;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVolume(Long volumeId, Long userId) {
        NovelVolume volume = novelVolumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "卷不存在");
        }
        getOwnedNovel(volume.getNovelId(), userId);
        // 级联删除该卷下的所有幕
        novelActMapper.delete(new LambdaQueryWrapper<NovelAct>().eq(NovelAct::getVolumeId, volumeId));
        novelVolumeMapper.deleteById(volumeId);
    }

    // ==================== Acts ====================

    @Override
    public List<NovelAct> getActs(Long novelId, Long userId) {
        getOwnedNovel(novelId, userId);
        return novelActMapper.selectList(
                new LambdaQueryWrapper<NovelAct>()
                        .eq(NovelAct::getNovelId, novelId)
                        .orderByAsc(NovelAct::getSortOrder));
    }

    @Override
    public List<NovelAct> getActsByVolume(Long volumeId, Long userId) {
        NovelVolume volume = novelVolumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "卷不存在");
        }
        getOwnedNovel(volume.getNovelId(), userId);
        return novelActMapper.selectList(
                new LambdaQueryWrapper<NovelAct>()
                        .eq(NovelAct::getVolumeId, volumeId)
                        .orderByAsc(NovelAct::getSortOrder));
    }

    @Override
    public NovelAct createAct(Long novelId, NovelActDTO dto, Long userId) {
        getOwnedNovel(novelId, userId);
        // 验证 volume 属于该 novel
        NovelVolume volume = novelVolumeMapper.selectById(dto.getVolumeId());
        if (volume == null || !volume.getNovelId().equals(novelId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "卷不存在或不属于该小说");
        }

        NovelAct act = new NovelAct();
        act.setNovelId(novelId);
        act.setVolumeId(dto.getVolumeId());
        act.setActNumber(dto.getActNumber() != null ? dto.getActNumber() : getNextActNumber(dto.getVolumeId()));
        act.setTitle(dto.getTitle());
        act.setOutline(dto.getOutline());
        act.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : act.getActNumber());
        act.setCreatedAt(LocalDateTime.now());
        act.setUpdatedAt(LocalDateTime.now());
        novelActMapper.insert(act);
        return act;
    }

    @Override
    public NovelAct updateAct(Long actId, NovelActDTO dto, Long userId) {
        NovelAct act = novelActMapper.selectById(actId);
        if (act == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "幕不存在");
        }
        getOwnedNovel(act.getNovelId(), userId);
        if (dto.getVolumeId() != null) act.setVolumeId(dto.getVolumeId());
        if (dto.getActNumber() != null) act.setActNumber(dto.getActNumber());
        if (dto.getTitle() != null) act.setTitle(dto.getTitle());
        if (dto.getOutline() != null) act.setOutline(dto.getOutline());
        if (dto.getSortOrder() != null) act.setSortOrder(dto.getSortOrder());
        act.setUpdatedAt(LocalDateTime.now());
        novelActMapper.updateById(act);
        return act;
    }

    @Override
    public void deleteAct(Long actId, Long userId) {
        NovelAct act = novelActMapper.selectById(actId);
        if (act == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "幕不存在");
        }
        getOwnedNovel(act.getNovelId(), userId);
        novelActMapper.deleteById(actId);
    }

    // ==================== Helper ====================

    private int getNextVolumeNumber(Long novelId) {
        Long count = novelVolumeMapper.selectCount(
                new LambdaQueryWrapper<NovelVolume>().eq(NovelVolume::getNovelId, novelId));
        return count.intValue() + 1;
    }

    private int getNextActNumber(Long volumeId) {
        Long count = novelActMapper.selectCount(
                new LambdaQueryWrapper<NovelAct>().eq(NovelAct::getVolumeId, volumeId));
        return count.intValue() + 1;
    }

    private Novel getOwnedNovel(Long novelId, Long userId) {
        Novel novel = novelMapper.selectById(novelId);
        if (novel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "小说不存在");
        }
        if (!userId.equals(novel.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人小说");
        }
        return novel;
    }

    private void updateDailyProgress(Long novelId, Long userId, Integer wordsWritten) {
        LocalDate today = LocalDate.now();
        NovelDailyProgress progress = novelDailyProgressMapper.selectOne(
                new LambdaQueryWrapper<NovelDailyProgress>()
                        .eq(NovelDailyProgress::getNovelId, novelId)
                        .eq(NovelDailyProgress::getProgressDate, today));
        if (progress == null) {
            progress = new NovelDailyProgress();
            progress.setNovelId(novelId);
            progress.setUserId(userId);
            progress.setProgressDate(today);
            progress.setWordsWritten(wordsWritten);
            progress.setChaptersCompleted(1);
            progress.setCreatedAt(LocalDateTime.now());
            progress.setUpdatedAt(LocalDateTime.now());
            novelDailyProgressMapper.insert(progress);
        } else {
            progress.setWordsWritten(progress.getWordsWritten() + wordsWritten);
            progress.setChaptersCompleted(progress.getChaptersCompleted() + 1);
            progress.setUpdatedAt(LocalDateTime.now());
            novelDailyProgressMapper.updateById(progress);
        }
    }
}
