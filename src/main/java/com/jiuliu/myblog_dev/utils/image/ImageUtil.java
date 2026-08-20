/*
 * [ImageUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/8/20
 */

package com.jiuliu.myblog_dev.utils.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片处理工具类
 *
 * <p>提供图片格式校验、尺寸读取、像素上限校验、缩放与无损压缩功能。</p>
 *
 * <p><b>安全说明（解压炸弹防护）：</b><br>
 * 尺寸读取只解析图片头部（ImageReader.getWidth/getHeight），不在解码前分配像素缓冲；
 * 上传入口必须先调用 {@link #isPixelCountExceeded(byte[])} 校验像素总量上限，
 * 防止压缩后体积很小但解压后占用 GB 级内存的恶意图片触发 OOM。</p>
 */
public class ImageUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageUtil.class);

    /**
     * 允许的最大图片大小（10MB）
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * JPEG 压缩质量（0.0 - 1.0），这里设为 0.95 保证较高质量
     */
    private static final float JPEG_QUALITY = 0.95f;

    /**
     * 4K 分辨率最大宽度（3840）
     */
    public static final int MAX_WIDTH_4K = 3840;

    /**
     * 4K 分辨率最大高度（2160）
     */
    public static final int MAX_HEIGHT_4K = 2160;

    /**
     * 像素总量上限（2500 万像素，约等于 6000x4000）。
     * 超过该上限的图片直接拒绝，防止"解压炸弹"（10MB 内压缩图片解码出 GB 级位图）导致 OOM。
     */
    public static final long MAX_PIXELS = 25_000_000L;

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
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")
                && !ext.equals("gif") && !ext.equals("bmp") && !ext.equals("webp")) {
            log.warn("不支持的图片格式：{}", ext);
            return false;
        }

        return isValidImageHeader(bytes);
    }

    /**
     * 校验图片像素总量是否超过上限（解码前只读头部，安全）
     *
     * @param bytes 图片字节数据
     * @return 超过上限返回 true；无法读取尺寸（如 WebP 无解码器）返回 false（不做服务端解码，原样存储）
     */
    public static boolean isPixelCountExceeded(byte[] bytes) {
        int[] dimensions = getImageDimensions(bytes);
        if (dimensions == null) {
            log.warn("无法读取图片尺寸，跳过像素上限校验（原样存储，不做服务端解码）");
            return false;
        }
        long pixels = (long) dimensions[0] * dimensions[1];
        if (pixels > MAX_PIXELS) {
            log.warn("图片像素总量超过上限：{}x{} = {} 像素 > {} 像素", dimensions[0], dimensions[1], pixels, MAX_PIXELS);
            return true;
        }
        return false;
    }

    /**
     * 获取图片尺寸
     *
     * <p>只读取图片头部元数据（不解码像素数据），因此对超大尺寸图片也是安全的。</p>
     *
     * @param bytes 图片字节数据
     * @return int[] 数组，index 0 为宽度，index 1 为高度；如果解析失败返回 null
     */
    public static int[] getImageDimensions(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                log.debug("没有找到可读取该图片格式的解码器");
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return new int[]{width, height};
            } catch (Exception e) {
                log.warn("读取图片头部尺寸失败：{}", e.getMessage());
                return null;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            log.warn("无法读取图片尺寸信息：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 如果图片分辨率超过 4K，则缩放至 4K
     * <p>缩放过程中保持宽高比和原始图片格式，使用高质量缩放算法。
     * 超过 {@link #MAX_PIXELS} 像素上限的图片应由调用方在上传入口拒绝，本方法不会解码超限图片。</p>
     *
     * @param bytes     图片字节数据
     * @param extension 原始文件扩展名（用于确定输出格式）
     * @return 缩放后的图片字节数据；如果图片不超过 4K 或缩放失败，返回原始数据
     */
    public static byte[] scaleTo4KIfNeeded(byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }

        int[] dimensions = getImageDimensions(bytes);
        if (dimensions == null) {
            log.warn("无法获取图片尺寸，返回原始数据");
            return bytes;
        }

        int originalWidth = dimensions[0];
        int originalHeight = dimensions[1];

        if (originalWidth <= MAX_WIDTH_4K && originalHeight <= MAX_HEIGHT_4K) {
            log.debug("图片分辨率 {}x{} 未超过 4K，无需缩放", originalWidth, originalHeight);
            return bytes;
        }

        log.info("图片分辨率 {}x{} 超过 4K，将进行缩放", originalWidth, originalHeight);

        double widthRatio = (double) MAX_WIDTH_4K / originalWidth;
        double heightRatio = (double) MAX_HEIGHT_4K / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = Math.max(1, (int) (originalWidth * ratio));
        int newHeight = Math.max(1, (int) (originalHeight * ratio));

        log.debug("计算缩放比例：{}，目标分辨率：{}x{}", ratio, newWidth, newHeight);

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(bais);
        } catch (IOException e) {
            log.error("无法读取原始图片进行缩放：{}", e.getMessage());
            return bytes;
        }

        if (originalImage == null) {
            log.warn("无法解析图片，返回原始数据");
            return bytes;
        }

        // 使用 ARGB 保留透明通道，避免透明 PNG 缩放后黑底
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String ext = extension != null ? extension.toLowerCase().replace(".", "") : "";
        String formatName = ("jpg".equals(ext) || "jpeg".equals(ext)) ? "jpg" : "png";

        try {
            ImageIO.write(resizedImage, formatName, baos);
        } catch (IOException e) {
            log.error("缩放后图片保存失败：{}", e.getMessage());
            return bytes;
        }

        byte[] scaledBytes = baos.toByteArray();
        log.info("图片缩放完成：原始分辨率 {}x{} ({} bytes) -> 缩放后分辨率 {}x{} ({} bytes)，格式={}",
                originalWidth, originalHeight, bytes.length,
                newWidth, newHeight, scaledBytes.length, formatName.toUpperCase());

        return scaledBytes;
    }

    /**
     * 如果图片分辨率超过 4K，则缩放至 4K（使用 PNG 格式输出）
     * <p>此方法已废弃，建议使用 {@link #scaleTo4KIfNeeded(byte[], String)} 保持原始格式</p>
     *
     * @param bytes 图片字节数据
     * @return 缩放后的图片字节数据；如果图片不超过 4K 或缩放失败，返回原始数据
     * @deprecated 请使用 {@link #scaleTo4KIfNeeded(byte[], String)}
     */
    @Deprecated
    public static byte[] scaleTo4KIfNeeded(byte[] bytes) {
        return scaleTo4KIfNeeded(bytes, "png");
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
        return bytes.length >= 12 &&
                bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 &&
                bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 &&
                bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 &&
                bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50;
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
        }
        if ("png".equals(ext)) {
            return compressPng(bytes);
        }

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
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
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
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();

            // PNG 不支持质量参数，但可以通过设置滤波器进行一定程度的压缩
            if (param.canWriteProgressive()) {
                param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            }

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }
}
