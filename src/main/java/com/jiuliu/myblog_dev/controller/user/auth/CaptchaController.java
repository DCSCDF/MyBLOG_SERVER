/*
 * [CaptchaController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

package com.jiuliu.myblog_dev.controller.user.auth;


import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.generator.common.model.dto.GenerateParam;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    private final ImageCaptchaApplication imageCaptchaApplication;

    public CaptchaController(ImageCaptchaApplication imageCaptchaApplication) {
        this.imageCaptchaApplication = imageCaptchaApplication;
    }

    /**
     * 获取行为验证码（限流：每 IP 每 1 分钟最多 10 次）
     */
    @PostMapping("/get")
    @RateLimit(count = 10, period = 1)
    public Response<ApiResponse<ImageCaptchaVO>> gen(
            @RequestParam(value = "type", required = false) String type) {
        if (type == null || type.isBlank()) {
            type = CaptchaTypeConstant.SLIDER;
        }
        GenerateParam generateParam = new GenerateParam();
        generateParam.setType(type);

        ApiResponse<ImageCaptchaVO> apiResponse = imageCaptchaApplication.generateCaptcha(generateParam);
        if (apiResponse.isSuccess()) {
            return ResponseUtil.success(apiResponse, 200);
        } else {
            return ResponseUtil.fail(apiResponse.getMsg(), apiResponse, 400);
        }
    }

    /**
     * 校验行为验证码（限流：每 IP 每 1 分钟最多 20 次）
     */
    @PostMapping("/check")
    @RateLimit(count = 20, period = 1)
    public Response<ApiResponse<?>> check(@RequestBody CheckRequest body) {
        try {
            ApiResponse<?> apiResponse = imageCaptchaApplication.matching(body.getId(), body.getData());
            if (apiResponse.isSuccess()) {
                return ResponseUtil.success(apiResponse, 200);
            } else {
                return ResponseUtil.fail(apiResponse.getMsg(), apiResponse, 400);
            }
        } catch (Exception e) {
            log.warn("验证码校验异常: {}", e.getMessage());
            return ResponseUtil.fail("验证码校验失败，请重新获取验证码", 400);
        }
    }

    /**
     * 二次验证（限流：每 IP 每 1 分钟最多 10 次）
     */
    @GetMapping("/verify")
    @RateLimit(count = 10, period = 1)
    public Response<?> verify(@RequestParam("id") String id) {
        if (imageCaptchaApplication instanceof SecondaryVerificationApplication secondary) {
            boolean success = secondary.secondaryVerification(id);
            if (success) {
                return ResponseUtil.success(Collections.singletonMap("id", id), 200);
            }
            return ResponseUtil.fail("验证码二次验证失败，请重新获取验证码", 400);
        }
        return ResponseUtil.fail("未开启二次验证功能", 400);
    }

    /**
     * TianAi-Captcha 校验请求体。
     */
    @Data
    public static class CheckRequest {
        private String id;
        private ImageCaptchaTrack data;
    }
}
