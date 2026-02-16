/*
 * [PageRoleResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import lombok.Data;

import java.util.List;

@Data
public class PageRoleResponseDTO {

    private List<RoleResponseDTO> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
}
