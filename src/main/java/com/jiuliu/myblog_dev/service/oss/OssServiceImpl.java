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
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.utils.image.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class OssServiceImpl implements OssService {

    private static final Logger log = LoggerFactory.getLogger(OssServiceImpl.class);

    private final OSSConfig ossConfig;

    public OssServiceImpl(OSSConfig ossConfig) {
        this.ossConfig = ossConfig;
    }

    @Override
    public SaResult testConnection() {
        log.debug("开始 OSS 连接测试...");

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
    public SaResult uploadImage(String fileName, byte[] fileBytes, String contentType) {
        log.debug("开始图片上传，原始文件名=[{}]，文件大小={} bytes，contentType=[{}]",
                fileName, fileBytes != null ? fileBytes.length : 0, contentType);

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
        log.debug("提取文件扩展名=[{}]", extension);

        // 2. 校验图片格式
        if (!ImageUtil.isValidFormat(fileBytes, extension)) {
            log.warn("图片上传失败：格式校验不通过，文件名=[{}]", fileName);
            return SaResult.error("不支持的图片格式或文件损坏，支持的格式：jpg, jpeg, png, gif, bmp, webp").setCode(400);
        }

        // 3. 无损压缩
        byte[] processedBytes;
        try {
            log.debug("开始图片压缩...");
            processedBytes = ImageUtil.compressImage(fileBytes, extension);
            log.debug("图片压缩完成，压缩后大小={} bytes", processedBytes.length);
        } catch (IOException e) {
            log.error("图片压缩失败：{}", e.getMessage(), e);
            return SaResult.error("图片处理失败：" + e.getMessage()).setCode(500);
        }

        // 4. 生成 OSS 对象名（路径）
        String objectName = generateObjectName(extension);
        log.debug("生成的 OSS 对象名=[{}]", objectName);

        // 5. 上传到 OSS
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(extension));
            metadata.setContentLength(processedBytes.length);

            log.debug("开始上传到 OSS，bucket=[{}]，objectName=[{}]，文件大小={} bytes",
                    ossConfig.getBucket(), objectName, processedBytes.length);

            ossClient.putObject(ossConfig.getBucket(), objectName, new java.io.ByteArrayInputStream(processedBytes), metadata);

            // 6. 返回图片 URL
            String imageUrl = ossConfig.getImageUrlPrefix() + objectName;
            log.info("图片上传成功，URL=[{}]，原始大小={} bytes，压缩后={} bytes",
                    imageUrl, fileBytes.length, processedBytes.length);

            return SaResult.data(new ImageUploadResponse(objectName, imageUrl, processedBytes.length));
        } catch (Exception e) {
            log.error("图片上传失败：{}", e.getMessage(), e);
            return SaResult.error("图片上传失败：" + e.getMessage()).setCode(500);
        }
    }

    @Override
    public SaResult deleteImage(String objectName) {
        log.debug("开始删除图片，objectName=[{}]", objectName);

        if (!ossConfig.isConfigured()) {
            log.warn("图片删除失败：OSS 配置未完成");
            return SaResult.error("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置").setCode(400);
        }

        if (objectName == null || objectName.isBlank()) {
            log.warn("图片删除失败：对象名为空");
            return SaResult.error("对象名称不能为空").setCode(400);
        }

        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("图片删除失败：无法获取 OSS 客户端");
            return SaResult.error("OSS 客户端初始化失败，请检查配置").setCode(500);
        }

        try {
            log.debug("执行删除操作，bucket=[{}]，objectName=[{}]", ossConfig.getBucket(), objectName);
            ossClient.deleteObject(ossConfig.getBucket(), objectName);
            log.info("图片删除成功，objectName=[{}]", objectName);
            return SaResult.ok().setMsg("删除成功");
        } catch (Exception e) {
            log.error("图片删除失败：{}", e.getMessage(), e);
            return SaResult.error("删除失败：" + e.getMessage()).setCode(500);
        }
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
     * 格式：images/yyyy/MM/dd/UUID.ext
     */
    private String generateObjectName(String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "images/" + datePath + "/" + uuid + "." + extension;
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
    public record ImageUploadResponse(String objectName, String url, long size) {}
}
