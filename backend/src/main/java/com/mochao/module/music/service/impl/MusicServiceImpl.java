package com.mochao.module.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.common.service.MinioService;
import com.mochao.module.music.entity.Music;
import com.mochao.module.music.mapper.MusicMapper;
import com.mochao.module.music.service.MusicService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MusicServiceImpl implements MusicService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String CATEGORY = "music";

    private final MusicMapper musicMapper;
    private final MinioService minioService;

    public MusicServiceImpl(MusicMapper musicMapper, MinioService minioService) {
        this.musicMapper = musicMapper;
        this.minioService = minioService;
    }

    @Override
    public Page<Music> getMusicList(int page, int size, Long userId) {
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Music::getUserId, userId)
               .orderByDesc(Music::getCreatedAt);
        return musicMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Music uploadMusic(MultipartFile file, String title, String artist, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择音乐文件");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件大小不能超过 50MB");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件名不能为空");
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "不支持的音频格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // 上传到 MinIO
        String objectKey = minioService.uploadFile(file, CATEGORY, userId);

        // 确定标题
        if (title == null || title.trim().isEmpty()) {
            title = originalFilename.replaceAll("\\.[^.]+$", "");
        }

        // 保存数据库记录
        Music music = new Music();
        music.setUserId(userId);
        music.setTitle(title.trim());
        music.setArtist(artist != null ? artist.trim() : "");
        music.setFileName(originalFilename);
        music.setFilePath(objectKey); // 存储 MinIO 对象 key
        music.setFileSize(file.getSize());
        music.setCreatedAt(LocalDateTime.now());
        music.setUpdatedAt(LocalDateTime.now());
        musicMapper.insert(music);

        return music;
    }

    @Override
    @Transactional
    public void deleteMusic(Long id, Long userId) {
        Music music = musicMapper.selectById(id);
        if (music == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "音乐文件不存在");
        }
        if (!userId.equals(music.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此音乐");
        }

        // 从 MinIO 删除文件
        minioService.deleteFile(music.getFilePath());

        // 删除数据库记录
        musicMapper.deleteById(id);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
    }
}
