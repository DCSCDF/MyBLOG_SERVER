/*
 * [SeoService.java]
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

package com.jiuliu.myblog_dev.service.seo;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.seo.PageSeoDTO;
import com.jiuliu.myblog_dev.dto.seo.SeoCreateDTO;
import com.jiuliu.myblog_dev.dto.seo.SeoUpdateDTO;

public interface SeoService {

    SaResult getPageSeos(PageSeoDTO pageDto);

    SaResult getSeoById(Long id);

    SaResult createSeo(SeoCreateDTO dto);

    SaResult updateSeo(SeoUpdateDTO dto);

    SaResult deleteSeo(Long id);
}
