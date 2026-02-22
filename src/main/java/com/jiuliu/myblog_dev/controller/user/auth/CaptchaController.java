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


import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    private final CaptchaService captchaService;

    // 构造函数注入
    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 获取验证码（限流：每 IP 每 1 分钟最多 10 次）
     */
    @PostMapping("/get")
    @RateLimit(count = 10, period = 1)
    public Response<ResponseModel> get(@RequestBody CaptchaVO captchaVO) {
        ResponseModel responseModel = captchaService.get(captchaVO);
        if (responseModel.isSuccess()) {
            return ResponseUtil.success(responseModel, 200);
        } else {
            return ResponseUtil.fail(responseModel.getRepMsg(), responseModel, 400);
        }
    }

    /**
     * 检查验证码（限流：每 IP 每 1 分钟最多 20 次）
     * 若前端传入的坐标等数据为 null 或字符串 "null"，验证码库会抛异常，此处统一捕获并返回 400。
     */
    @PostMapping("/check")
    @RateLimit(count = 20, period = 1)
    public Response<ResponseModel> check(@RequestBody CaptchaVO captchaVO) {
        try {
            ResponseModel responseModel = captchaService.check(captchaVO);
            if (responseModel.isSuccess()) {
                return ResponseUtil.success(responseModel, 200);
            } else {
                return ResponseUtil.fail(responseModel.getRepMsg(), responseModel, 400);
            }
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("验证码校验请求数据格式错误（如坐标为 null 或非数字），请前端传合法 point 数据: {}", e.getMessage());
            return ResponseUtil.fail("验证码数据格式错误，请重新滑动验证码", 400);
        } catch (Exception e) {
            log.warn("验证码校验异常: {}", e.getMessage());
            return ResponseUtil.fail("验证码校验失败，请重新获取验证码", 400);
        }
    }

    /**
     * 二次验证（限流：每 IP 每 1 分钟最多 10 次）
     * 若请求数据格式异常，验证码库可能抛异常，此处统一捕获并返回 400。
     */
    @PostMapping("/verify")
    @RateLimit(count = 10, period = 1)
    public Response<ResponseModel> verify(@RequestBody CaptchaVO captchaVO) {
        try {
            ResponseModel responseModel = captchaService.verification(captchaVO);
            if (responseModel.isSuccess()) {
                return ResponseUtil.success(responseModel, 200);
            } else {
                return ResponseUtil.fail(responseModel.getRepMsg(), responseModel, 400);
            }
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("验证码二次验证请求数据格式错误: {}", e.getMessage());
            return ResponseUtil.fail("验证码数据格式错误，请重新验证", 400);
        } catch (Exception e) {
            log.warn("验证码二次验证异常: {}", e.getMessage());
            return ResponseUtil.fail("验证码验证失败，请重新获取验证码", 400);
        }
    }
}
