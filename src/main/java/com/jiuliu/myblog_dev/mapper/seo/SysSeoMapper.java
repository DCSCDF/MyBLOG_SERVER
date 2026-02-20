/*
 * [SysSeoMapper.java]
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

package com.jiuliu.myblog_dev.mapper.seo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.seo.SysSeo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysSeoMapper extends BaseMapper<SysSeo> {

    /**
     * 查询未删除的 SEO 配置中所有不重复的页面类型名称（用于筛选项，类型名称不重复）
     */
    @Select("SELECT DISTINCT page_type FROM sys_seo WHERE is_deleted = 0 ORDER BY page_type")
    List<String> selectDistinctPageTypes();
}
