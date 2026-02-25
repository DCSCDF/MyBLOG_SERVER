/*
 * [SysFriendLinkMapper.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/23
 */

package com.jiuliu.myblog_dev.mapper.link;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.link.SysFriendLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFriendLinkMapper extends BaseMapper<SysFriendLink> {
}

