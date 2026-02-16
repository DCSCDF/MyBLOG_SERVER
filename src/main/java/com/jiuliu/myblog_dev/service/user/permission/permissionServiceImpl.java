/*
 * [permissionServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/24 23:24
 */

package com.jiuliu.myblog_dev.service.user.permission;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionDTO;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permission.PermissionResponseDTO;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class permissionServiceImpl implements permissionService {

    private static final Logger log = LoggerFactory.getLogger(permissionServiceImpl.class);

    private final SysPermissionMapper sysPermissionMapper;

    // 构造函数注入
    public permissionServiceImpl(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }


    @Override
    public SaResult getPagePermissions(PagePermissionDTO pageDto) {
//        log.info("分页获取权限列表，currentPage={}, pageSize={}", pageDto.getCurrentPage(), pageDto.getPageSize());

        try {
            // 创建分页对象
            Page<SysPermission> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());

            // 构建查询条件，按sort_order降序排列
            QueryWrapper<SysPermission> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("sort_order");

            // 执行分页查询
            Page<SysPermission> pageResult = sysPermissionMapper.selectPage(page, queryWrapper);

            // 转换实体为DTO
            List<PermissionResponseDTO> permissionDTOs = pageResult.getRecords().stream()
                    .map(this::convertToPermissionResponseDTO)
                    .collect(Collectors.toList());

            // 构建分页响应DTO
            PagePermissionResponseDTO responseDTO = new PagePermissionResponseDTO();
            responseDTO.setRecords(permissionDTOs);
            responseDTO.setTotal(pageResult.getTotal());
            responseDTO.setSize(pageResult.getSize());
            responseDTO.setCurrent(pageResult.getCurrent());
            responseDTO.setPages(pageResult.getPages());

//            log.info("成功获取权限分页列表，共{}条记录，总页数{}", permissionDTOs.size(), pageResult.getPages());
            return SaResult.data(responseDTO);
        } catch (Exception e) {
            log.error("分页获取权限列表异常", e);
            return SaResult.error("分页获取权限列表失败").setCode(500);
        }
    }

    /**
     * 将SysPermission实体转换为PermissionResponseDTO
     */
    private PermissionResponseDTO convertToPermissionResponseDTO(SysPermission permission) {
        PermissionResponseDTO dto = new PermissionResponseDTO();
        dto.setId(permission.getId());
        dto.setCode(permission.getCode());
        dto.setName(permission.getName());
        dto.setDescription(permission.getDescription());
        dto.setSortOrder(permission.getSortOrder());
        dto.setCreateTime(permission.getCreateTime());
        return dto;
    }


}
