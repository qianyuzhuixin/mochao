package com.mochao.module.music.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.music.entity.Music;
import com.mochao.module.music.service.MusicService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/music")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping
    public Result<Page<Music>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(musicService.getMusicList(page, size, userId));
    }

    @GetMapping("/favorites")
    public Result<Page<Music>> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(musicService.getFavoriteMusicList(page, size, userId));
    }

    @PostMapping("/{id}/favorite")
    public Result<Music> toggleFavorite(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(musicService.toggleFavorite(id, userId));
    }

    @PostMapping("/upload")
    public Result<Music> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "artist", required = false) String artist) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(musicService.uploadMusic(file, title, artist, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        musicService.deleteMusic(id, userId);
        return Result.success();
    }
}
