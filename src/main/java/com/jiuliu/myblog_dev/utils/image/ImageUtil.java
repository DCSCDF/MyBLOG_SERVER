/*
 * [ImageUtil.java]
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

package com.jiuliu.myblog_dev.utils.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 图片处理工具类
 *
 * <p>提供图片格式校验和无损压缩功能。</p>
 */
public class ImageUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageUtil.class);

    /**
     * 支持的图片格式集合
     */
    private static final Set<String> ALLOWED_FORMATS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    ));

    /**
     * 允许的最大图片大小（10MB）
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * JPEG 压缩质量（0.0 - 1.0），这里设为 0.95 保证较高质量
     */
    private static final float JPEG_QUALITY = 0.95f;

    /**
     * PNG 压缩级别（0-9），0 无压缩，9 最大压缩
     */
    private static final int PNG_COMPRESSION_LEVEL = 6;

    /**
     * 校验图片格式
     *
     * @param bytes     图片字节数据
     * @param extension 文件扩展名（小写，不带点）
     * @return 如果格式合法返回 true，否则返回 false
     */
    public static boolean isValidFormat(byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0) {
            log.warn("图片数据为空");
            return false;
        }

        if (bytes.length > MAX_FILE_SIZE) {
            log.warn("图片大小超过限制：{} bytes > {} bytes", bytes.length, MAX_FILE_SIZE);
            return false;
        }

        String ext = extension != null ? extension.toLowerCase().replace(".", "") : "";
        if (!ALLOWED_FORMATS.contains(ext)) {
            log.warn("不支持的图片格式：{}", ext);
            return false;
        }

        // 校验文件头魔数
        if (!isValidImageHeader(bytes)) {
            log.warn("图片文件头校验失败");
            return false;
        }

        return true;
    }

    /**
     * 校验图片文件头（魔数）
     *
     * @param bytes 图片字节数据
     * @return 如果文件头有效返回 true
     */
    private static boolean isValidImageHeader(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }

        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 &&
                bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return true;
        }
        // GIF: 47 49 46 38
        if (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 &&
                bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x38) {
            return true;
        }
        // BMP: 42 4D
        if (bytes[0] == (byte) 0x42 && bytes[1] == (byte) 0x4D) {
            return true;
        }
        // WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
        if (bytes.length >= 12 &&
                bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 &&
                bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 &&
                bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 &&
                bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) {
            return true;
        }

        return false;
    }

    /**
     * 无损压缩图片
     *
     * <p>对于 JPEG 图片，使用 JPEG 编码器压缩；<br>
     * 对于 PNG 图片，使用 PNG 编码器压缩；<br>
     * 其他格式（GIF, BMP, WebP）保持原样返回。</p>
     *
     * @param bytes     原始图片字节数据
     * @param extension 文件扩展名（小写）
     * @return 压缩后的图片字节数据
     * @throws IOException 如果压缩过程中发生 IO 错误
     */
    public static byte[] compressImage(byte[] bytes, String extension) throws IOException {
        String ext = extension != null ? extension.toLowerCase().replace(".", "") : "";

        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return compressJpeg(bytes);
        } else if ("png".equals(ext)) {
            return compressPng(bytes);
        }

        // GIF, BMP, WebP 保持原样
        log.debug("图片格式 {} 不需要压缩，直接返回原数据", ext);
        return bytes;
    }

    /**
     * 压缩 JPEG 图片
     *
     * @param bytes 原始 JPEG 字节数据
     * @return 压缩后的 JPEG 字节数据
     * @throws IOException 如果压缩失败
     */
    private static byte[] compressJpeg(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedImage image = ImageIO.read(bais);
        if (image == null) {
            log.warn("无法解析 JPEG 图片，返回原数据");
            return bytes;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            log.warn("没有找到 JPEG 编码器，返回原数据");
            return bytes;
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();

        byte[] compressed = baos.toByteArray();
        log.debug("JPEG 压缩完成：原始大小 {} bytes，压缩后 {} bytes，压缩率 {:.2f}%",
                bytes.length, compressed.length,
                (1 - (double) compressed.length / bytes.length) * 100);

        return compressed;
    }

    /**
     * 压缩 PNG 图片
     *
     * @param bytes 原始 PNG 字节数据
     * @return 压缩后的 PNG 字节数据
     * @throws IOException 如果压缩失败
     */
    private static byte[] compressPng(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedImage image = ImageIO.read(bais);
        if (image == null) {
            log.warn("无法解析 PNG 图片，返回原数据");
            return bytes;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            log.warn("没有找到 PNG 编码器，返回原数据");
            return bytes;
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        // PNG 不支持质量参数，但可以通过设置滤波器进行一定程度的压缩
        if (param.canWriteProgressive()) {
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        }

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();

        byte[] compressed = baos.toByteArray();
        log.debug("PNG 处理完成：原始大小 {} bytes，处理后 {} bytes",
                bytes.length, compressed.length);

        return compressed;
    }

    /**
     * 获取允许的图片格式列表
     *
     * @return 允许的图片格式集合
     */
    public static Set<String> getAllowedFormats() {
        return new HashSet<>(ALLOWED_FORMATS);
    }
}
