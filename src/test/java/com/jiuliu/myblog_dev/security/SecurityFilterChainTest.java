/*
 * [SecurityFilterChainTest.java]
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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 过滤器链行为测试：受保护接口未登录返回统一 401 JSON，
 * 公开接口不被登录拦截（与迁移前的公开/受保护划分一致）。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointWithoutTokenReturnsUnified401() throws Exception {
        mockMvc.perform(post("/api/auth/profile").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/blogs/list")
                        .header("token", "invalid-token-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void logoutWithoutLoginKeepsBusiness401Body() throws Exception {
        // 登出接口在过滤器链放行，由业务层返回原有 401 文案（HTTP 状态仍为 200，与迁移前一致）
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.errorMsg").value("用户未登录或会话已过期"));
    }

    @Test
    void loginEndpointIsPublicAndFailsOnBusinessValidation() throws Exception {
        // 登录接口在过滤器链放行：空参数应进入业务层并以业务 400 返回，而非被鉴权拦截为 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(not(401)))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void corsPreflightIsAllowedWithoutLogin() throws Exception {
        mockMvc.perform(options("/api/blogs/list")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "token,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", is("http://localhost:5173")));
    }

    @Test
    void publicEndpointIsNotBlockedByLogin() throws Exception {
        // 公开接口应越过登录拦截进入业务层；H2 测试库缺少 sys_seo 表时业务层返回 503，
        // 关键断言是“不是 401”，证明未被鉴权拦截
        mockMvc.perform(get("/api/public/seo"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401) {
                        throw new AssertionError("公开接口不应被登录拦截，实际返回 401");
                    }
                });
    }
}
