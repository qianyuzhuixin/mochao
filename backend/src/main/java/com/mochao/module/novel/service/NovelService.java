package com.mochao.module.novel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.novel.dto.*;
import com.mochao.module.novel.entity.*;

import java.util.List;
import java.util.Map;

public interface NovelService {

    // Novel CRUD
    Page<Novel> getNovelList(Long userId, Integer page, Integer size, String status);
    Novel getNovelById(Long id, Long userId);
    Novel createNovel(NovelCreateDTO dto, Long userId);
    Novel updateNovel(Long id, NovelUpdateDTO dto, Long userId);
    void deleteNovel(Long id, Long userId);

    // Progress
    Map<String, Object> getNovelProgress(Long id, Long userId);

    // Outline
    NovelOutline getOutline(Long novelId, Long userId);
    NovelOutline updateOutline(Long novelId, String content, Long userId);

    // Worldview
    NovelWorldview getWorldview(Long novelId, Long userId);
    NovelWorldview updateWorldview(Long novelId, String content, Long userId);

    // Characters
    List<NovelCharacter> getCharacters(Long novelId, Long userId);
    NovelCharacter createCharacter(Long novelId, NovelCharacterDTO dto, Long userId);
    NovelCharacter updateCharacter(Long characterId, NovelCharacterDTO dto, Long userId);
    void deleteCharacter(Long characterId, Long userId);

    // Items
    List<NovelItem> getItems(Long novelId, Long userId);
    NovelItem createItem(Long novelId, NovelItemDTO dto, Long userId);
    NovelItem updateItem(Long itemId, NovelItemDTO dto, Long userId);
    void deleteItem(Long itemId, Long userId);

    // Chapter Outlines
    List<NovelChapterOutline> getChapterOutlines(Long novelId, Long userId);
    NovelChapterOutline createChapterOutline(Long novelId, NovelChapterOutlineDTO dto, Long userId);
    NovelChapterOutline updateChapterOutline(Long outlineId, NovelChapterOutlineDTO dto, Long userId);
    void deleteChapterOutline(Long outlineId, Long userId);

    // Volume Outlines（卷纲）
    List<NovelVolume> getVolumes(Long novelId, Long userId);
    NovelVolume createVolume(Long novelId, NovelVolumeDTO dto, Long userId);
    NovelVolume updateVolume(Long volumeId, NovelVolumeDTO dto, Long userId);
    void deleteVolume(Long volumeId, Long userId);

    // Acts（幕）
    List<NovelAct> getActs(Long novelId, Long userId);
    List<NovelAct> getActsByVolume(Long volumeId, Long userId);
    NovelAct createAct(Long novelId, NovelActDTO dto, Long userId);
    NovelAct updateAct(Long actId, NovelActDTO dto, Long userId);
    void deleteAct(Long actId, Long userId);

    // Chapters
    Page<NovelChapter> getChapters(Long novelId, Long userId, Integer page, Integer size);
    NovelChapter getChapter(Long chapterId, Long userId);
    NovelChapter createChapter(Long novelId, NovelChapterDTO dto, Long userId);
    NovelChapter updateChapter(Long chapterId, NovelChapterDTO dto, Long userId);
    void deleteChapter(Long chapterId, Long userId);
}
