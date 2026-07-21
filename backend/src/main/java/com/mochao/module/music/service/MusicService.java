package com.mochao.module.music.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.music.entity.Music;
import org.springframework.web.multipart.MultipartFile;

public interface MusicService {

    Page<Music> getMusicList(int page, int size, Long userId);

    Page<Music> getFavoriteMusicList(int page, int size, Long userId);

    Music uploadMusic(MultipartFile file, String title, String artist, Long userId);

    void deleteMusic(Long id, Long userId);

    Music toggleFavorite(Long id, Long userId);

}
