/*
 * [RoleServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.role;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.user.role.*;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final SysRoleMapper sysRoleMapper;

    public RoleServiceImpl(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public SaResult getPageRoles(PageRoleDTO pageDto) {
        try {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getIsDeleted, 0)
                    .orderByDesc(SysRole::getSortOrder);

            Page<SysRole> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysRole> pageResult = sysRoleMapper.selectPage(page, wrapper);

            List<RoleResponseDTO> dtos = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            PageRoleResponseDTO response = new PageRoleResponseDTO();
            response.setRecords(dtos);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取角色列表异常", e);
            return SaResult.error("获取角色列表失败").setCode(500);
        }
    }

    @Override
    public SaResult getRoleById(Long id) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getId, id)
                        .eq(SysRole::getIsDeleted, 0));
        if (role == null) {
            return SaResult.error("角色不存在").setCode(404);
        }
        return SaResult.data(toResponseDTO(role));
    }

    @Override
    public SaResult updateRole(RoleUpdateDTO dto) {
        SysRole role = sysRoleMapper.selectById(dto.getId());
        if (role == null) {
            return SaResult.error("角色不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            return SaResult.error("系统内置角色不可修改").setCode(403);
        }

        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, dto.getId())
                .set(SysRole::getName, dto.getName())
                .set(dto.getDescription() != null, SysRole::getDescription, dto.getDescription())
                .set(dto.getSortOrder() != null, SysRole::getSortOrder, dto.getSortOrder())
                .set(dto.getStatus() != null, SysRole::getStatus, dto.getStatus());

        sysRoleMapper.update(null, wrapper);
        log.info("角色更新成功，id={}", dto.getId());
        return SaResult.data(toResponseDTO(sysRoleMapper.selectById(dto.getId())));
    }

    @Override
    public SaResult deleteRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            return SaResult.error("角色不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            return SaResult.error("系统内置角色不可删除").setCode(403);
        }

        sysRoleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, id)
                .set(SysRole::getIsDeleted, 1));
        log.info("角色逻辑删除成功，id={}", id);
        return SaResult.data("删除成功");
    }

    private RoleResponseDTO toResponseDTO(SysRole role) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setSuperAdmin(role.getSuperAdmin());
        dto.setIsSystem(role.getIsSystem());
        dto.setSortOrder(role.getSortOrder());
        dto.setStatus(role.getStatus());
        dto.setCreateTime(role.getCreateTime());
        dto.setUpdateTime(role.getUpdateTime());
        return dto;
    }
}
