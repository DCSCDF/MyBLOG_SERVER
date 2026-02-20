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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    // 构造函数注入
    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 获取验证码
     */
    @PostMapping("/get")
//    @RateLimit(count = 6, period = 5)
    public Response<ResponseModel> get(@RequestBody CaptchaVO captchaVO) {
        ResponseModel responseModel = captchaService.get(captchaVO);
        if (responseModel.isSuccess()) {
            return ResponseUtil.success(responseModel, 200);
        } else {
            return ResponseUtil.fail(responseModel.getRepMsg(), responseModel, 400);
        }
    }

    /**
     * 检查验证码
     */
    @PostMapping("/check")
    public Response<ResponseModel> check(@RequestBody CaptchaVO captchaVO) {
        ResponseModel responseModel = captchaService.check(captchaVO);
        if (responseModel.isSuccess()) {
            return ResponseUtil.success(responseModel, 200);
        } else {
            return ResponseUtil.fail(responseModel.getRepMsg(), responseModel, 400);
        }
    }

    /**
     * 二次验证
     */
    @PostMapping("/verify")
    public Response<ResponseModel> verify(@RequestBody CaptchaVO captchaVO) {
        ResponseModel responseModel = captchaService.verification(captchaVO);
        if (responseModel.isSuccess()) {
            return ResponseUtil.success(responseModel, 200);
        } else {
            return ResponseUtil.fail(responseModel.getRepMsg(), responseModel, 400);
        }
    }
}
