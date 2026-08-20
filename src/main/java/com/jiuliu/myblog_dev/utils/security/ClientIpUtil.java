/*
 * [ClientIpUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * license: "MIT"
 * UpdateTime: 2026/8/20
 */

package com.jiuliu.myblog_dev.utils.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 客户端真实 IP 解析工具
 *
 * <p>修复 X-Forwarded-For / X-Real-IP 无条件信任导致的限流绕过漏洞：</p>
 * <ul>
 *   <li>配置了 {@code app.security.trusted-proxies}（逗号分隔的可信反向代理 IP）时，
 *       仅当请求直接来源（remoteAddr）命中可信代理列表才信任转发头；</li>
 *   <li>未配置时，仅当直接来源为回环/内网地址（本机 Nginx、Docker 同机部署场景）
 *       才信任转发头，应用直连公网时一律使用 remoteAddr，杜绝伪造。</li>
 * </ul>
 */
@Component
public class ClientIpUtil {

    private final Set<String> trustedProxies;

    public ClientIpUtil(@Value("${app.security.trusted-proxies:}") String trustedProxiesConfig) {
        this.trustedProxies = new HashSet<>();
        if (StringUtils.hasText(trustedProxiesConfig)) {
            Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(this.trustedProxies::add);
        }
    }

    /**
     * 解析客户端真实 IP
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = request.getRemoteAddr();

        boolean fromTrustedSource;
        if (!trustedProxies.isEmpty()) {
            fromTrustedSource = trustedProxies.contains(remoteAddr);
        } else {
            // 未显式配置可信代理时：仅信任来自回环/内网来源的转发头（本机或同机 Docker 部署）
            fromTrustedSource = isPrivateOrLoopback(remoteAddr);
        }

        if (fromTrustedSource) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
                return xff.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
                return realIp;
            }
        }

        return remoteAddr;
    }

    /**
     * 判断是否为回环地址或 RFC1918 内网地址
     */
    private static boolean isPrivateOrLoopback(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        // IPv4 内网段
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
