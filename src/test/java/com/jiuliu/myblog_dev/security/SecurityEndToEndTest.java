/*
 * [SecurityEndToEndTest.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 */

package com.jiuliu.myblog_dev.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端鉴权测试：真实令牌 -> TokenAuthenticationFilter ->
 * H2 中的用户/角色/权限 -> @PreAuthorize。
 * 超级管理员可访问仅 SUPER_ADMIN 的监控接口，普通用户得到统一 403。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/security-e2e.sql")
class SecurityEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTokenService authTokenService;

    @Test
    void superAdminTokenPassesRoleCheck() throws Exception {
        String token = authTokenService.login(1L, -1);
        mockMvc.perform(get("/api/admin/monitor/memory").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void profileReturnsCurrentLoggedInUser() throws Exception {
        String token = authTokenService.login(1L, -1);
        mockMvc.perform(post("/api/auth/profile").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("root"));
    }

    @Test
    void permissionsEndpointReturnsGrantedPermissionCodes() throws Exception {
        // 普通用户经 sys_role_permission 直接获得 article:list，接口应原样返回该权限码
        String token = authTokenService.login(2L, -1);
        mockMvc.perform(post("/api/auth/permissions").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").value(hasItem("article:list")));
    }

    @Test
    void normalUserTokenIsForbiddenFromAdminMonitor() throws Exception {
        String token = authTokenService.login(2L, -1);
        mockMvc.perform(get("/api/admin/monitor/memory").header("token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.errorMsg").value("没有权限"));
    }

    @Test
    void normalUserTokenIsForbiddenFromArticleDelete() throws Exception {
        // 普通用户拥有 article:list 但没有 article:delete，应被 @PreAuthorize 拦截为统一 403
        String token = authTokenService.login(2L, -1);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/blogs/1").header("token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
