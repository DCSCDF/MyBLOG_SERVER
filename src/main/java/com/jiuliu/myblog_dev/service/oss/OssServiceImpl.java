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
 * UpdateTime: 2026/4/4
 */

package com.jiuliu.myblog_dev.service.oss;

import cn.dev33.satoken.util.SaResult;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.dto.oss.PageUserOssDTO;
import com.jiuliu.myblog_dev.dto.oss.PageUserOssResponseDTO;
import com.jiuliu.myblog_dev.dto.oss.UserOssResponseDTO;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    /**
     * 用户OSS列表缓存
     * 缓存时间：5分钟
     */
    private final Cache<String, PageUserOssResponseDTO> userOssListCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

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
            return SaResult.error("OSS 配置已完成，但无法连接到服务器，请检查网络与配置").setCode(400);
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

        // 3.5 校验像素总量上限（解码前只读头部，防止解压炸弹 OOM）
        if (ImageUtil.isPixelCountExceeded(fileBytes)) {
            log.warn("图片上传失败：像素总量超过上限，文件名=[{}]，大小={} bytes", fileName, fileBytes.length);
            return SaResult.error("图片分辨率过高，像素总量不能超过 2500 万").setCode(400);
        }

        // 4. 检查并缩放分辨率超过 4K 的图片
        byte[] resizedBytes = ImageUtil.scaleTo4KIfNeeded(fileBytes, extension);

        // 5. 无损压缩（对缩放后的图片进行压缩）
        byte[] processedBytes;
        try {
//            log.debug("开始图片压缩...");
            processedBytes = ImageUtil.compressImage(resizedBytes, extension);
//            log.debug("图片压缩完成，压缩后大小={} bytes", processedBytes.length);
        } catch (Exception e) {
            log.error("图片压缩失败：{}", e.getMessage(), e);
            return SaResult.error("图片处理失败，请稍后重试").setCode(500);
        }

        // 6. 计算 MD5 哈希值
        String hash = calculateMD5(processedBytes);
        if (hash == null) {
            log.error("图片哈希计算失败");
            return SaResult.error("图片哈希计算失败").setCode(500);
        }
//        log.debug("图片 MD5 哈希=[{}]", hash);

        // 7. 检查哈希是否已存在（防止重复上传）
        SysOssImage existingImage = sysOssImageMapper.selectByHash(hash);
        if (existingImage != null) {
            log.info("图片已存在，跳过重复上传，hash=[{}]，objectName=[{}]", hash, existingImage.getObjectName());
            return SaResult.data(new ImageUploadResponse(
                    hash,
                    existingImage.getOriginalName(),
                    existingImage.getFileSize()
            ));
        }

        // 8. 生成新文件名
        // 格式：原始名称（截断至128位）_时间戳_文件大小_16位随机字符.扩展名
        String originalNameTruncated = truncateFileName(fileName);
        long timestamp = Instant.now().toEpochMilli();
        long fileSize = processedBytes.length;
        String randomChars = generateRandomChars();
        String newFileName = String.format("%s_%d_%d_%s.%s",
                originalNameTruncated, timestamp, fileSize, randomChars, extension);
//        log.debug("生成的新文件名=[{}]", newFileName);

        // 9. 生成 OSS 对象名
        String objectName = generateObjectName(newFileName);
//        log.debug("生成的 OSS 对象名=[{}]", objectName);

        // 10. 上传到 OSS
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
            return SaResult.error("图片上传失败，请稍后重试").setCode(500);
        }

        // 11. 保存到数据库
        try {
            SysOssImage ossImage = new SysOssImage();
            ossImage.setHash(hash);
            ossImage.setOriginalName(originalNameTruncated);
            ossImage.setObjectName(objectName);
            ossImage.setFileSize(fileSize);
            ossImage.setUserId(userId);

            sysOssImageMapper.insert(ossImage);
//            log.info("图片记录已保存到数据库，hash=[{}]，objectName=[{}]", hash, objectName);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发上传同一内容：唯一索引冲突。清理本次上传的 OSS 对象，返回已存在记录（幂等成功）
            log.warn("图片哈希已存在（并发上传），hash=[{}]，objectName=[{}]", hash, objectName);
            try {
                ossClient.deleteObject(ossConfig.getBucket(), objectName);
            } catch (Exception deleteEx) {
                log.warn("清理重复上传的 OSS 对象失败：{}，objectName={}", deleteEx.getMessage(), objectName);
            }
            SysOssImage existing = sysOssImageMapper.selectByHash(hash);
            if (existing != null) {
                eventPublisher.publishEvent(new OssImageChangedEvent(this, OssImageChangedEvent.EventType.UPLOAD, hash, userId));
                return SaResult.data(new ImageUploadResponse(
                        existing.getHash(),
                        existing.getOriginalName(),
                        existing.getFileSize()
                ));
            }
            return SaResult.error("图片上传失败，请稍后重试").setCode(500);
        } catch (Exception e) {
            log.error("保存图片记录失败：{}", e.getMessage(), e);
            // OSS 上传成功但数据库保存失败，尝试删除 OSS 文件
            try {
                ossClient.deleteObject(ossConfig.getBucket(), objectName);
                log.warn("已删除因数据库保存失败而上传的 OSS 文件，objectName=[{}]", objectName);
            } catch (Exception deleteEx) {
                log.error("删除 OSS 文件失败：{}", deleteEx.getMessage());
            }
            return SaResult.error("图片上传失败，请稍后重试").setCode(500);
        }

        // 12. 发布图片变更事件（清除缓存）
        eventPublisher.publishEvent(new OssImageChangedEvent(this, OssImageChangedEvent.EventType.UPLOAD, hash, userId));

        // 13. 返回结果
        String imageUrl = ossConfig.getImageUrl(objectName);
        log.info("图片上传全部完成，hash=[{}]，URL=[{}]，原始大小={} bytes，处理后={} bytes",
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
            return SaResult.error("删除 OSS 图片失败，请稍后重试").setCode(500);
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
        eventPublisher.publishEvent(new OssImageChangedEvent(this, OssImageChangedEvent.EventType.DELETE, objectName, userId));

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

    @Override
    public SaResult getPageUserOssImages(PageUserOssDTO dto, Long userId) {
        try {
            if (userId == null) {
                log.warn("获取用户图片列表失败：用户ID为空");
                return SaResult.error("用户未登录").setCode(401);
            }

            // 构建缓存键
            String cacheKey = "user_oss_list_" + userId + "_" + dto.getCurrentPage() + "_" +
                    dto.getPageSize() + "_" + (dto.getKeyword() != null ? dto.getKeyword() : "");

            // 尝试从缓存获取
            PageUserOssResponseDTO cached = userOssListCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取用户OSS图片列表，key={}", cacheKey);
                return SaResult.data(cached);
            }

            // 构建查询条件
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOssImage> queryWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

            // 必须是当前用户的图片
            queryWrapper.eq(SysOssImage::getUserId, userId);

            // 关键词搜索：文件名、哈希值
            if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
                queryWrapper.and(wrapper -> wrapper
                        .like(SysOssImage::getOriginalName, dto.getKeyword())
                        .or()
                        .like(SysOssImage::getHash, dto.getKeyword())
                );
            }

            // 按创建时间倒序
            queryWrapper.orderByDesc(SysOssImage::getCreateTime);

            // 分页查询
            com.baomidou.mybatisplus.core.metadata.IPage<SysOssImage> pageResult =
                    sysOssImageMapper.selectPage(
                            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                                    dto.getCurrentPage(), dto.getPageSize()),
                            queryWrapper
                    );

            // 转换为响应DTO
            List<UserOssResponseDTO> records = pageResult.getRecords().stream()
                    .map(this::convertToUserOssResponseDTO)
                    .collect(Collectors.toList());

            // 构建响应
            PageUserOssResponseDTO response = new PageUserOssResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());

            // 存入缓存
            userOssListCache.put(cacheKey, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取用户OSS图片列表异常", e);
            return SaResult.error("获取图片列表失败").setCode(500);
        }
    }

    /**
     * 清除指定用户的OSS列表缓存
     */
    public void clearUserOssCache(Long userId) {
        userOssListCache.asMap().keySet().removeIf(key -> key.startsWith("user_oss_list_" + userId + "_"));
        log.debug("已清除用户OSS列表缓存，userId={}", userId);
    }

    /**
     * 将实体转换为用户OSS响应DTO
     *
     * <p>填充多尺寸图片访问 URL，方便前端根据不同场景选择合适的尺寸。</p>
     *
     * @param image 图片实体
     * @return 用户OSS响应DTO
     */
    private UserOssResponseDTO convertToUserOssResponseDTO(SysOssImage image) {
        UserOssResponseDTO dto = new UserOssResponseDTO();
        dto.setId(image.getId());
        dto.setHash(image.getHash());
        dto.setOriginalName(image.getOriginalName());
        dto.setObjectName(image.getObjectName());
        dto.setFileSize(image.getFileSize());
        dto.setCreateTime(image.getCreateTime());

        // 填充多尺寸图片 URL
        // small 使用小图尺寸 (256px 宽)
        // large 使用大图尺寸 (1080px 宽)
        OSSConfig.ImageUrls urls = ossConfig.getAllSizeImageUrls(image.getObjectName());
        dto.setSmallUrl(urls.small());
        dto.setLargeUrl(urls.large());
        dto.setUrl(urls.original());

        return dto;
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
