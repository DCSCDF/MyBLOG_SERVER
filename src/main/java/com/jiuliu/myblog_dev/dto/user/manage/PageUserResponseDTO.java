/*
 * [PageUserResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import lombok.Data;

import java.util.List;

@Data
public class PageUserResponseDTO {

    private List<UserAdminResponseDTO> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
}

