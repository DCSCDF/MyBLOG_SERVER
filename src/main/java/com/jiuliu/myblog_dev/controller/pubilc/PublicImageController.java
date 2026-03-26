/*
 * [PublicImageController.java]
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

package com.jiuliu.myblog_dev.controller.pubilc;

import com.jiuliu.myblog_dev.service.oss.ImageService;
import com.jiuliu.myblog_dev.service.oss.ImageService.ImageResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开图片获取接口
 *
 * <p>通过哈希值获取 OSS 中的图片。</p>
 */
@RestController
@RequestMapping("/api/images")
public class PublicImageController {

//    private static final Logger log = LoggerFactory.getLogger(PublicImageController.class);

    private final ImageService imageService;

    public PublicImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 通过哈希值获取图片
     *
     * @param hash 图片哈希值（MD5）
     * @return 图片数据
     */
    @GetMapping("/{hash}")
    public ResponseEntity<byte[]> getImage(@PathVariable String hash) {
//        log.debug("获取图片请求，hash=[{}]", hash);

        ImageResult result = imageService.getImageByHash(hash);

        if (!result.found() || result.data() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(result.extension()));
        headers.setContentLength(result.data().length);
        headers.setCacheControl("public, max-age=1800");

        return new ResponseEntity<>(result.data(), headers, HttpStatus.OK);
    }

    /**
     * 根据扩展名获取 MIME 类型
     */
    private MediaType getMediaType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "bmp" -> MediaType.parseMediaType("image/bmp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
