/*
 * [OssServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.service.oss;

import cn.dev33.satoken.util.SaResult;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.entity.oss.SysOssImage;
import com.jiuliu.myblog_dev.event.OssImageChangedEvent;
import com.jiuliu.myblog_dev.mapper.oss.SysOssImageMapper;
import com.jiuliu.myblog_dev.utils.image.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OssServiceImpl implements OssService {

    private static final Logger log = LoggerFactory.getLogger(OssServiceImpl.class);

    /**
     * 允许的最大图片大小（10MB）
     */
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    /**
     * 原始文件名的最大长度
     */
    private static final int MAX_ORIGINAL_NAME_LENGTH = 128;

    private final OSSConfig ossConfig;
    private final SysOssImageMapper sysOssImageMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OssServiceImpl(OSSConfig ossConfig, SysOssImageMapper sysOssImageMapper,
                          ApplicationEventPublisher eventPublisher) {
        this.ossConfig = ossConfig;
        this.sysOssImageMapper = sysOssImageMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SaResult testConnection() {
//        log.debug("开始 OSS 连接测试...");

        if (!ossConfig.isConfigured()) {
            log.warn("OSS 连接测试失败：配置未完成");
            return SaResult.error("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置").setCode(400);
        }

        log.debug("OSS 配置检查通过 - bucket=[{}], endpoint=[{}]",
                ossConfig.getBucket(), ossConfig.getEndpoint());

        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("OSS 连接测试失败：无法获取 OSS 客户端");
            return SaResult.error("OSS 客户端初始化失败，请检查配置").setCode(500);
        }

        try {
            String bucket = ossConfig.getBucket();
            log.debug("尝试列出 Bucket 中的对象，bucket=[{}]", bucket);
            ossClient.listObjects(bucket);
            log.info("OSS 连接测试成功，bucket={}", bucket);
            return SaResult.data("OSS 配置已完成且连接正常");
        } catch (Exception e) {
            log.warn("OSS 连接测试失败：{}", e.getMessage());
            log.warn("OSS 连接错误类型：{}", e.getClass().getName());
            return SaResult.error("OSS 配置已完成，但无法连接到服务器：" + e.getMessage()).setCode(400);
        }
    }

    @Override
    public SaResult uploadImage(String fileName, byte[] fileBytes, String contentType, Long userId) {
        log.debug("开始图片上传，原始文件名=[{}]，文件大小={} bytes，contentType=[{}]，userId=[{}]",
                fileName, fileBytes != null ? fileBytes.length : 0, contentType, userId);

        // 0. 参数校验
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("图片上传失败：文件数据为空");
            return SaResult.error("文件数据为空").setCode(400);
        }

        if (!ossConfig.isConfigured()) {
            log.warn("图片上传失败：OSS 配置未完成");
            return SaResult.error("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置").setCode(400);
        }

        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("图片上传失败：无法获取 OSS 客户端");
            return SaResult.error("OSS 客户端初始化失败，请检查配置").setCode(500);
        }

        // 1. 提取文件扩展名
        String extension = getFileExtension(fileName);
//        log.debug("提取文件扩展名=[{}]", extension);

        // 2. 校验图片大小（不超过 10MB）
        if (fileBytes.length > MAX_IMAGE_SIZE) {
            log.warn("图片上传失败：文件大小超过限制 {} bytes", fileBytes.length);
            return SaResult.error("图片大小不能超过 10MB").setCode(400);
        }

        // 3. 校验图片格式
        if (!ImageUtil.isValidFormat(fileBytes, extension)) {
            log.warn("图片上传失败：格式校验不通过，文件名=[{}]", fileName);
            return SaResult.error("不支持的图片格式或文件损坏，支持的格式：jpg, jpeg, png, gif, bmp, webp").setCode(400);
        }

        // 4. 无损压缩
        byte[] processedBytes;
        try {
//            log.debug("开始图片压缩...");
            processedBytes = ImageUtil.compressImage(fileBytes, extension);
//            log.debug("图片压缩完成，压缩后大小={} bytes", processedBytes.length);
        } catch (IOException e) {
            log.error("图片压缩失败：{}", e.getMessage(), e);
            return SaResult.error("图片处理失败：" + e.getMessage()).setCode(500);
        }

        // 5. 计算 MD5 哈希值
        String hash = calculateMD5(processedBytes);
        if (hash == null) {
            log.error("图片哈希计算失败");
            return SaResult.error("图片哈希计算失败").setCode(500);
        }
//        log.debug("图片 MD5 哈希=[{}]", hash);

        // 6. 检查哈希是否已存在（防止重复上传）
        SysOssImage existingImage = sysOssImageMapper.selectByHash(hash);
        if (existingImage != null) {
            log.info("图片已存在，跳过重复上传，hash=[{}]，objectName=[{}]", hash, existingImage.getObjectName());
            return SaResult.data(new ImageUploadResponse(
                    hash,
                    existingImage.getOriginalName(),
                    existingImage.getFileSize()
            ));
        }

        // 7. 生成新文件名
        // 格式：原始名称（截断至128位）_时间戳_文件大小_16位随机字符.扩展名
        String originalNameTruncated = truncateFileName(fileName);
        long timestamp = Instant.now().toEpochMilli();
        long fileSize = processedBytes.length;
        String randomChars = generateRandomChars();
        String newFileName = String.format("%s_%d_%d_%s.%s",
                originalNameTruncated, timestamp, fileSize, randomChars, extension);
//        log.debug("生成的新文件名=[{}]", newFileName);

        // 8. 生成 OSS 对象名
        String objectName = generateObjectName(newFileName);
//        log.debug("生成的 OSS 对象名=[{}]", objectName);

        // 9. 上传到 OSS
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(extension));
            metadata.setContentLength(processedBytes.length);

//            log.debug("开始上传到 OSS，bucket=[{}]，objectName=[{}]，文件大小={} bytes",
//                    ossConfig.getBucket(), objectName, processedBytes.length);

            ossClient.putObject(ossConfig.getBucket(), objectName,
                    new java.io.ByteArrayInputStream(processedBytes), metadata);

//            log.info("图片上传到 OSS 成功，objectName=[{}]，大小={} bytes", objectName, processedBytes.length);
        } catch (Exception e) {
            log.error("图片上传到 OSS 失败：{}", e.getMessage(), e);
            return SaResult.error("图片上传失败：" + e.getMessage()).setCode(500);
        }

        // 10. 保存到数据库
        try {
            SysOssImage ossImage = new SysOssImage();
            ossImage.setHash(hash);
            ossImage.setOriginalName(originalNameTruncated);
            ossImage.setObjectName(objectName);
            ossImage.setFileSize(fileSize);
            ossImage.setUserId(userId);

            sysOssImageMapper.insert(ossImage);
//            log.info("图片记录已保存到数据库，hash=[{}]，objectName=[{}]", hash, objectName);
        } catch (Exception e) {
            log.error("保存图片记录失败：{}", e.getMessage(), e);
            // OSS 上传成功但数据库保存失败，尝试删除 OSS 文件
            try {
                ossClient.deleteObject(ossConfig.getBucket(), objectName);
                log.warn("已删除因数据库保存失败而上传的 OSS 文件，objectName=[{}]", objectName);
            } catch (Exception deleteEx) {
                log.error("删除 OSS 文件失败：{}", deleteEx.getMessage());
            }
            return SaResult.error("图片上传成功但保存记录失败：" + e.getMessage()).setCode(500);
        }

        // 11. 发布图片变更事件（清除缓存）
        eventPublisher.publishEvent(new OssImageChangedEvent(this, OssImageChangedEvent.EventType.UPLOAD, hash));

        // 12. 返回结果
        String imageUrl = ossConfig.getImageUrlPrefix() + objectName;
        log.info("图片上传全部完成，hash=[{}]，URL=[{}]，原始大小={} bytes，压缩后={} bytes",
                hash, imageUrl, fileBytes.length, processedBytes.length);

        return SaResult.data(new ImageUploadResponse(
                hash,
                originalNameTruncated,
                fileSize
        ));
    }

    @Override
    public SaResult deleteImage(String objectName, Long userId) {
        log.debug("开始删除图片，objectName=[{}]，userId=[{}]", objectName, userId);

        if (!ossConfig.isConfigured()) {
            log.warn("图片删除失败：OSS 配置未完成");
            return SaResult.error("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置").setCode(400);
        }

        if (objectName == null || objectName.isBlank()) {
            log.warn("图片删除失败：对象名为空");
            return SaResult.error("对象名称不能为空").setCode(400);
        }

        // 1. 从数据库查询图片记录
        SysOssImage imageRecord = sysOssImageMapper.selectByObjectName(objectName);
        if (imageRecord == null) {
            log.warn("图片记录不存在，objectName=[{}]", objectName);
            return SaResult.error("图片记录不存在").setCode(404);
        }

        // 2. 校验用户权限：只有上传该图片的用户才能删除
        if (userId == null || !userId.equals(imageRecord.getUserId())) {
            log.warn("图片删除失败：权限不足，objectName=[{}]，图片上传者=[{}]，请求删除者=[{}]",
                    objectName, imageRecord.getUserId(), userId);
            return SaResult.error("无权限删除此图片").setCode(403);
        }

//        String hash = imageRecord.getHash();
//        log.debug("找到图片记录，hash=[{}]，objectName=[{}]", hash, objectName);

        // 3. 删除 OSS 中的图片
        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("图片删除失败：无法获取 OSS 客户端");
            return SaResult.error("OSS 客户端初始化失败，请检查配置").setCode(500);
        }

        try {
//            log.debug("执行删除操作，bucket=[{}]，objectName=[{}]", ossConfig.getBucket(), objectName);
            ossClient.deleteObject(ossConfig.getBucket(), objectName);
//            log.info("OSS 图片删除成功，objectName=[{}]", objectName);
        } catch (Exception e) {
            log.error("OSS 图片删除失败：{}", e.getMessage(), e);
            return SaResult.error("删除 OSS 图片失败：" + e.getMessage()).setCode(500);
        }

        // 4. 删除数据库记录
        try {
            sysOssImageMapper.deleteById(imageRecord.getId());
//            log.info("数据库图片记录删除成功，id=[{}]，hash=[{}]", imageRecord.getId(), hash);
        } catch (Exception e) {
            log.error("数据库图片记录删除失败：{}", e.getMessage(), e);
            // OSS 已删除，但数据库记录删除失败（不应发生）
            return SaResult.error("OSS 图片已删除，但数据库记录删除失败").setCode(500);
        }

        // 5. 发布图片变更事件（清除缓存）
        eventPublisher.publishEvent(new OssImageChangedEvent(this, OssImageChangedEvent.EventType.DELETE, objectName));

        return SaResult.ok().setMsg("删除成功");
    }

    @Override
    public SaResult deleteImageByHash(String hash, Long userId) {
        log.debug("开始删除图片（通过哈希），hash=[{}]，userId=[{}]", hash, userId);

        if (hash == null || hash.isBlank()) {
            log.warn("图片删除失败：哈希值为空");
            return SaResult.error("哈希值不能为空").setCode(400);
        }

        // 1. 从数据库查询图片记录
        SysOssImage imageRecord = sysOssImageMapper.selectByHash(hash);
        if (imageRecord == null) {
            log.warn("图片记录不存在，hash=[{}]", hash);
            return SaResult.error("图片记录不存在").setCode(404);
        }

        return deleteImage(imageRecord.getObjectName(), userId);
    }

    /**
     * 计算文件的 MD5 哈希值
     */
    private String calculateMD5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 算法不存在：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 截断文件名（移除扩展名，截断至128位后再加回扩展名）
     */
    private String truncateFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        String nameWithoutExt = fileName;
        String ext = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            ext = fileName.substring(dotIndex);
        }
        if (nameWithoutExt.length() > MAX_ORIGINAL_NAME_LENGTH) {
            nameWithoutExt = nameWithoutExt.substring(0, MAX_ORIGINAL_NAME_LENGTH);
        }
        return nameWithoutExt + ext;
    }

    /**
     * 生成 16 位随机字符
     */
    private String generateRandomChars() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 从文件名提取扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 生成 OSS 对象名
     * 格式：images/yyyy/MM/dd/文件名
     */
    private String generateObjectName(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "images/" + datePath + "/" + fileName;
    }

    /**
     * 根据扩展名获取 MIME 类型
     */
    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 图片上传响应
     */
    public record ImageUploadResponse(
            String hash,
            String originalName,
            Long size
    ) {
    }
}
