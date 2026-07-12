package com.mochao.module.music.controller;

import com.mochao.common.service.MinioService;
import com.mochao.module.music.entity.Music;
import com.mochao.module.music.mapper.MusicMapper;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;

/**
 * 文件流式代理控制器
 * <p>
 * 前端通过 /files/music/{id} 访问音乐文件，
 * 后端根据音乐 ID 查库获取 MinIO 对象 key，再流式返回。
 * <p>
 * 使用 ID 而非文件路径作为 URL，避免 URL 中出现 .mp3 等媒体扩展名，
 * 防止 IDM 等下载器扩展拦截 <audio> 元素的请求。
 * <p>
 * 支持 HTTP Range 请求（音频拖动进度条）。
 * Audio 元素无法携带 JWT Header，因此该路径在 SecurityConfig 中设为 permitAll。
 */
@Slf4j
@RestController
@RequestMapping("/v1/files")
public class FileController {

    private final MinioService minioService;
    private final MusicMapper musicMapper;

    public FileController(MinioService minioService, MusicMapper musicMapper) {
        this.minioService = minioService;
        this.musicMapper = musicMapper;
    }

    @GetMapping("/music/{id}")
    public ResponseEntity<StreamingResponseBody> streamMusic(
            @PathVariable Long id,
            HttpServletRequest request) {

        // 通过 ID 查库获取文件路径
        Music music = musicMapper.selectById(id);
        if (music == null) {
            return ResponseEntity.notFound().build();
        }
        String objectKey = music.getFilePath();
        if (objectKey == null || objectKey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 获取文件元信息
            StatObjectResponse stat = minioService.getStat(objectKey);
            long fileSize = stat.size();
            String contentType = stat.contentType() != null ? stat.contentType() : "audio/mpeg";

            // 解析 Range 头
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // 处理 Range 请求 → 206 Partial Content
                String range = rangeHeader.substring(6);
                String[] parts = range.split("-");
                long start = Long.parseLong(parts[0]);
                long end = parts.length > 1 && !parts[1].isEmpty()
                        ? Long.parseLong(parts[1])
                        : fileSize - 1;

                // 钳制范围
                if (start >= fileSize) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                            .build();
                }
                if (end >= fileSize) end = fileSize - 1;
                long contentLength = end - start + 1;

                StreamingResponseBody body = outputStream -> {
                    try (InputStream inputStream = minioService.getFile(objectKey)) {
                        // 跳过 start 字节
                        long skipped = 0;
                        while (skipped < start) {
                            long s = inputStream.skip(start - skipped);
                            if (s <= 0) break;
                            skipped += s;
                        }
                        // 写入 contentLength 字节
                        byte[] buffer = new byte[8192];
                        long remaining = contentLength;
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buffer.length, remaining);
                            int read = inputStream.read(buffer, 0, toRead);
                            if (read < 0) break;
                            outputStream.write(buffer, 0, read);
                            remaining -= read;
                        }
                        outputStream.flush();
                    }
                };

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE,
                                "bytes " + start + "-" + end + "/" + fileSize)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .contentLength(contentLength)
                        .body(body);
            } else {
                // 完整文件 → 200 OK
                StreamingResponseBody body = outputStream -> {
                    try (InputStream inputStream = minioService.getFile(objectKey)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, read);
                        }
                        outputStream.flush();
                    }
                };

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .contentLength(fileSize)
                        .body(body);
            }
        } catch (Exception e) {
            log.error("Failed to stream file '{}': {}", objectKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
