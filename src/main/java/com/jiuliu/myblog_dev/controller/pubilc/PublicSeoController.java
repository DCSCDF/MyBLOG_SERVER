/*
 * [PublicSeoController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.seo.PublicSeoResponseDTO;
import com.jiuliu.myblog_dev.service.seo.PublicSeoService;
import com.jiuliu.myblog_dev.utils.disabled.Disabled;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共SEO接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/seo")
public class PublicSeoController {

    private final PublicSeoService publicSeoService;

    public PublicSeoController(PublicSeoService publicSeoService) {
        this.publicSeoService = publicSeoService;
    }

    /**
     * 通用方法：处理SaResult结果
     */
    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }

    /**
     * 根据页面类型获取SEO配置
     * GET /api/public/seo
     * 无需登录，所有用户均可访问
     */
    @Disabled
    @GetMapping
    public Response<PublicSeoResponseDTO> getSeoByPageType(
            @RequestParam String pageType,
            @RequestParam(required = false) Long pageId) {
        SaResult saResult = publicSeoService.getSeoByPageType(pageType, pageId);
        return handleSaResult(saResult);
    }
}
