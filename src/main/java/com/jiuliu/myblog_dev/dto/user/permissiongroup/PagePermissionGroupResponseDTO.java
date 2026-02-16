/*
 * [PagePermissionGroupResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.permissiongroup;

import lombok.Data;

import java.util.List;

@Data
public class PagePermissionGroupResponseDTO {

    private List<PermissionGroupResponseDTO> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
}
