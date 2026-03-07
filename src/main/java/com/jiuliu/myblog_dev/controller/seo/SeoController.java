/*
 * [SeoController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/21
 */

package com.jiuliu.myblog_dev.controller.seo;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.seo.*;
import com.jiuliu.myblog_dev.service.seo.SeoService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seo")
public class SeoController {

    private final SeoService seoService;

    public SeoController(SeoService seoService) {
        this.seoService = seoService;
    }

    /**
     * 分页获取SEO列表
     * 权限：seo:list
     */
    @SaCheckPermission("system:seo:list")
    @PostMapping("/list")
    public Response<PageSeoResponseDTO> getPageSeos(@Valid @RequestBody PageSeoDTO pageDto) {
        SaResult saResult = seoService.getPageSeos(pageDto);
        return handleSaResult(saResult);
    }

    /**
     * 根据ID获取SEO详情
     * 权限：seo:list
     */
    @SaCheckPermission("system:seo:list")
    @GetMapping("/{id}")
    public Response<SeoResponseDTO> getSeoById(@PathVariable Long id) {
        SaResult saResult = seoService.getSeoById(id);
        return handleSaResult(saResult);
    }

    /**
     * 创建SEO配置
     * 权限：seo:create
     */
    @SaCheckPermission("system:seo:create")
    @PostMapping
    public Response<SeoResponseDTO> createSeo(@Valid @RequestBody SeoCreateDTO dto) {
        SaResult saResult = seoService.createSeo(dto);
        return handleSaResult(saResult);
    }

    /**
     * 修改SEO配置（系统内置的也可以编辑）
     * 权限：seo:edit
     */
    @SaCheckPermission("system:seo:edit")
    @PutMapping("/{id}")
    public Response<SeoResponseDTO> updateSeo(@PathVariable Long id, @Valid @RequestBody SeoUpdateDTO dto) {
        dto.setId(id);
        SaResult saResult = seoService.updateSeo(dto);
        return handleSaResult(saResult);
    }

    /**
     * 删除SEO配置（系统内置的不可删除）
     * 权限：seo:delete
     */
    @SaCheckPermission("system:seo:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteSeo(@PathVariable Long id) {
        SaResult saResult = seoService.deleteSeo(id);
        return handleSaResult(saResult);
    }

    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}
