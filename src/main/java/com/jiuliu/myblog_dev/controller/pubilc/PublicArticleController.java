/*
 * [PublicArticleController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/14 17:22
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleDTO;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleResponseDTO;
import com.jiuliu.myblog_dev.service.blog.PublicArticleService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共文章接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/article")
public class PublicArticleController {

    private final PublicArticleService publicArticleService;

    public PublicArticleController(PublicArticleService publicArticleService) {
        this.publicArticleService = publicArticleService;
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
     * 分页获取公共文章列表
     * POST /api/public/article/list
     * 无需登录，所有用户均可访问
     */
    @PostMapping("/list")
    public Response<PagePublicArticleResponseDTO> getPagePublicArticles(@Valid @RequestBody PagePublicArticleDTO dto) {
        SaResult saResult = publicArticleService.getPagePublicArticles(dto);
        return handleSaResult(saResult);
    }
}
