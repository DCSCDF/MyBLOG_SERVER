/*
 * [SysBlogLikeMapper.java]
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

package com.jiuliu.myblog_dev.mapper.blog.bloglike;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface SysBlogLikeMapper extends BaseMapper<SysRole> {
}