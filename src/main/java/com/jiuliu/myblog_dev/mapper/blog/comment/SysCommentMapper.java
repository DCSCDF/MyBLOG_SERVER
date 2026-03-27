/*
 * [SysCommentMapper.java]
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

package com.jiuliu.myblog_dev.mapper.blog.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface SysCommentMapper extends BaseMapper<SysComment> {

    /**
     * 统计指定文章的通过评论数量（包括子评论）
     * status = 1 表示通过，is_deleted = 0 表示未删除
     *
     * @param blogId 文章ID
     * @return 通过的评论数量
     */
    default int countApprovedComments(@Param("blogId") Long blogId) {
        return Math.toIntExact(selectCount(
                new LambdaQueryWrapper<SysComment>()
                        .eq(SysComment::getBlogId, blogId)
                        .eq(SysComment::getStatus, (byte) 1)
                        .eq(SysComment::getIsDeleted, 0)
        ));
    }
}