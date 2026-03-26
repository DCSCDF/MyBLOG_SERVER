/*
 * [SysOssImageMapper.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.mapper.oss;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.oss.SysOssImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * OSS 图片映射 Mapper
 */
@Mapper
public interface SysOssImageMapper extends BaseMapper<SysOssImage> {

    /**
     * 根据哈希值查询图片记录
     *
     * @param hash 图片哈希值
     * @return 图片记录
     */
    @Select("SELECT * FROM sys_oss_image WHERE hash = #{hash}")
    SysOssImage selectByHash(String hash);

    /**
     * 根据对象名称查询图片记录
     *
     * @param objectName OSS 对象名称
     * @return 图片记录
     */
    @Select("SELECT * FROM sys_oss_image WHERE object_name = #{objectName}")
    SysOssImage selectByObjectName(String objectName);
}
