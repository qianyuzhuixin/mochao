package com.mochao.common.service;

import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 对象存储服务
 * <p>
 * 统一封装文件上传、删除、读取操作。
 * 对象 key 规范：{category}/{date}/{userId}/{uuid}.{ext}
 * 例如：music/2026-07-12/1/a3b4c5d6-xxxx.mp3
 */
@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 上传文件到 MinIO
     *
     * @param file       Spring MultipartFile
     * @param category   文件分类（如 "music"）
     * @param userId     用户 ID
     * @return 对象 key（相对路径），如 music/2026-07-12/1/uuid.mp3
     */
    public String uploadFile(MultipartFile file, String category, Long userId) {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String objectKey = buildObjectKey(category, userId, extension);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("File uploaded to MinIO: {}/{}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传到对象存储失败");
        }
    }

    /**
     * 从 MinIO 读取文件流
     *
     * @param objectKey 对象 key
     * @return InputStream
     */
    public InputStream getFile(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get file from MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在或已被删除");
        }
    }

    /**
     * 获取文件元信息（用于获取 Content-Type、文件大小等）
     */
    public StatObjectResponse getStat(String objectKey) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            log.error("Failed to stat file from MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
    }

    /**
     * 删除文件
     *
     * @param objectKey 对象 key
     */
    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            log.info("File deleted from MinIO: {}/{}", bucket, objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {} - {}", objectKey, e.getMessage());
            // 删除失败不抛异常，避免影响数据库操作
        }
    }

    /**
     * 构建对象 key
     * 格式：{category}/{date}/{userId}/{uuid}.{ext}
     */
    private String buildObjectKey(String category, Long userId, String extension) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String uuid = UUID.randomUUID().toString();
        return String.format("%s/%s/%d/%s.%s", category, dateStr, userId, uuid, extension);
    }

    private String getExtension(String filename) {
        if (filename == null) return "bin";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "bin";
    }
}
