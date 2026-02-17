/*
 * [PermissionServiceImpl.java]
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionDTO;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permission.PermissionResponseDTO;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import org.springframework.util.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final SysPermissionMapper sysPermissionMapper;

    public PermissionServiceImpl(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public SaResult getPagePermissions(PagePermissionDTO pageDto) {
        try {
            LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                    .orderByDesc(SysPermission::getSortOrder);
            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysPermission::getCode, kw)
                        .or().like(SysPermission::getName, kw)
                        .or().like(SysPermission::getDescription, kw));
            }

            Page<SysPermission> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysPermission> pageResult = sysPermissionMapper.selectPage(page, wrapper);

            List<PermissionResponseDTO> permissionDTOs = pageResult.getRecords().stream()
                    .map(this::convertToPermissionResponseDTO)
                    .collect(Collectors.toList());

            PagePermissionResponseDTO responseDTO = new PagePermissionResponseDTO();
            responseDTO.setRecords(permissionDTOs);
            responseDTO.setTotal(pageResult.getTotal());
            responseDTO.setSize(pageResult.getSize());
            responseDTO.setCurrent(pageResult.getCurrent());
            responseDTO.setPages(pageResult.getPages());
            responseDTO.setFilterOptions(Collections.emptyMap());

            return SaResult.data(responseDTO);
        } catch (Exception e) {
            log.error("分页获取权限列表异常", e);
            return SaResult.error("分页获取权限列表失败").setCode(500);
        }
    }

    private PermissionResponseDTO convertToPermissionResponseDTO(SysPermission permission) {
        return getPermissionResponseDTO(permission);
    }

    @NonNull
    public static PermissionResponseDTO getPermissionResponseDTO(SysPermission permission) {
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
