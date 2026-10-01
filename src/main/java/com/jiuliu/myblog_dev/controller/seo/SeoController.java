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

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.seo.*;
import com.jiuliu.myblog_dev.service.seo.SeoService;
import com.jiuliu.myblog_dev.utils.disabled.Disabled;
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
    @Disabled
    @PreAuthorize("hasAuthority('system:seo:list')")
    @PostMapping("/list")
    public Response<PageSeoResponseDTO> getPageSeos(@Valid @RequestBody PageSeoDTO pageDto) {
        ServiceResult serviceResult = seoService.getPageSeos(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 根据ID获取SEO详情
     * 权限：seo:list
     */
    @Disabled
    @PreAuthorize("hasAuthority('system:seo:list')")
    @GetMapping("/{id}")
    public Response<SeoResponseDTO> getSeoById(@PathVariable Long id) {
        ServiceResult serviceResult = seoService.getSeoById(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 创建SEO配置
     * 权限：seo:create
     */
    @Disabled
    @PreAuthorize("hasAuthority('system:seo:create')")
    @PostMapping
    public Response<SeoResponseDTO> createSeo(@Valid @RequestBody SeoCreateDTO dto) {
        ServiceResult serviceResult = seoService.createSeo(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 修改SEO配置（系统内置的也可以编辑）
     * 权限：seo:edit
     */
    @Disabled
    @PreAuthorize("hasAuthority('system:seo:edit')")
    @PutMapping("/{id}")
    public Response<SeoResponseDTO> updateSeo(@PathVariable Long id, @Valid @RequestBody SeoUpdateDTO dto) {
        dto.setId(id);
        ServiceResult serviceResult = seoService.updateSeo(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除SEO配置（系统内置的不可删除）
     * 权限：seo:delete
     */
    @Disabled
    @PreAuthorize("hasAuthority('system:seo:delete')")
    @DeleteMapping("/{id}")
    public Response<Object> deleteSeo(@PathVariable Long id) {
        ServiceResult serviceResult = seoService.deleteSeo(id);
        return handleServiceResult(serviceResult);
    }

    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }
}
