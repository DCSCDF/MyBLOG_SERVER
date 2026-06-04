/*
 * [GlobalExceptionHandler.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8 04:37
 */

package com.jiuliu.myblog_dev.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.utils.disabled.DisabledException;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimitException;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLSyntaxErrorException;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Jackson 序列化工具，用于直接把 Response 写入 HttpServletResponse，
     * 彻底绕开 Spring 的内容协商（避免因客户端 Accept 头不匹配导致二次抛异常）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将统一响应体直接写入 HttpServletResponse，绕开 Spring 的内容协商与 HttpMessageConverter。
     * 当客户端 Accept 头无法匹配任何支持的媒体类型（如爬虫、扫描器请求），
     * 常规的返回 Response 对象的方式会再次触发 HttpMediaTypeNotAcceptableException，
     * 导致异常处理器被判定为失败，转而进入 Spring 默认错误页。
     * 此方法通过手动设置 Content-Type 并直接输出 JSON 字节解决该问题。
     */
    private void writeJsonResponse(HttpServletResponse response, int httpStatus, Response<Void> body) {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(body));
            writer.flush();
        } catch (IOException e) {
            // 写入失败时仅记录，不再抛出，避免无限循环
            log.error("写入异常响应失败: {}", e.getMessage());
        }
    }

    /**
     * 处理媒体类型不可接受异常（客户端 Accept 头不匹配）
     * 典型来源：爬虫、扫描器发送奇怪的 Accept 头。
     * 直接写 JSON 到 response，绕开内容协商。
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @SuppressWarnings("unused")
    public void handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e,
            HttpServletResponse response) {
        log.warn("Accept头不匹配: {}", e.getMessage());
        writeJsonResponse(response, 406, ResponseUtil.fail("不支持的响应格式", 406));
    }

    /**
     * 处理参数校验失败（@Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldError();
        String message = (firstError != null) ? firstError.getDefaultMessage() : "请求参数格式错误";
        log.warn("参数校验失败: {}", message);
        return ResponseUtil.fail(message, 400);
    }

    /**
     * 处理业务逻辑异常
     */
    @ExceptionHandler(BusinessException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {} (code: {})", e.getMessage(), e.getCode());
        return ResponseUtil.fail(e.getMessage(), e.getCode());
    }

    /**
     * 处理业务逻辑中的非法参数（如密码错误、用户不存在等）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("业务参数错误: {}", e.getMessage());
        return ResponseUtil.fail(e.getMessage(), 400);
    }

    /**
     * 处理 Sa-Token 鉴权异常
     */
    @ExceptionHandler({ NotLoginException.class, NotRoleException.class, NotPermissionException.class })
    @SuppressWarnings("unused")
    public Response<Void> handleAuthException(Exception e, HttpServletRequest request) {
        log.warn("鉴权异常: {}", e.getClass().getSimpleName());

        // // 如果是OPTIONS请求且是登录异常，直接放行
        // if (e instanceof NotLoginException &&
        // "OPTIONS".equalsIgnoreCase(request.getMethod())) {
        // log.debug("OPTIONS预检请求，跳过登录检查");
        // return SaResult.ok();
        // }

        // 不记录 e.toString() 或堆栈，避免泄露内部信息
        // 400: '请求参数错误',
        // 401: '未授权，请重新登录',
        // 403: '拒绝访问',
        // 404: '请求的资源不存在',
        // 408: '请求超时',
        // 429: '请求过于频繁',
        // 500: '服务器内部错误',
        // 502: '网关错误',
        // 503: '服务不可用',
        // 504: '网关超时'

        if (e instanceof NotLoginException) {
            return ResponseUtil.fail("未授权，请先登录", 401);
        } else if (e instanceof NotRoleException) {
            return ResponseUtil.fail("没有权限", 403);
        } else if (e instanceof NotPermissionException) {
            return ResponseUtil.fail("没有权限", 403);
        }
        return ResponseUtil.fail("鉴权失败", 403);
    }

    /**
     * 处理限流异常（返回 429，并设置 Retry-After 头供客户端退避）
     */
    @ExceptionHandler(RateLimitException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleRateLimitException(RateLimitException e, HttpServletResponse response) {
        log.warn("触发限流: {}", e.getMessage());
        response.setHeader("Retry-After", "60"); // 建议 60 秒后重试
        return ResponseUtil.fail(e.getMessage(), 429); // HTTP 429 Too Many Requests
    }

    /**
     * 处理接口禁用异常（返回 503）
     */
    @ExceptionHandler(DisabledException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleDisabledException(DisabledException e) {
        log.warn("接口被禁用: {}", e.getMessage());
        return ResponseUtil.fail(e.getMessage(), e.getCode());
    }

    /**
     * 处理非法状态异常（如 RateLimit 在非 Web 上下文中使用）
     */
    @ExceptionHandler(IllegalStateException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("非法状态: {}", e.getMessage());
        return ResponseUtil.fail("服务暂时不可用，请稍后再试", 500);
    }

    /**
     * 处理唯一键/唯一约束冲突（如角色编码、权限组名称等重复）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("sys_role.code")) {
            log.warn("角色编码重复: {}", msg);
            return ResponseUtil.fail("角色编码已存在", 400);
        }
        if (msg.contains("sys_permission.code")) {
            log.warn("权限编码重复: {}", msg);
            return ResponseUtil.fail("权限编码已存在", 400);
        }
        if (msg.contains("sys_permission_group") && msg.contains("name")) {
            log.warn("权限组名称重复: {}", msg);
            return ResponseUtil.fail("权限组名称已存在", 400);
        }
        log.warn("唯一约束冲突: {}", msg);
        return ResponseUtil.fail("数据已存在，请勿重复提交", 400);
    }

    /**
     * 处理其他未预期的系统异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause.getMessage() != null ? cause.getMessage() : "请求数据格式错误";
        log.warn("JSON解析失败: {}", message);
        return ResponseUtil.fail("请求数据格式错误，请检查JSON格式", 400);
    }

    /**
     * 处理数据库访问异常（给出更明确的错误提示）
     */
    @ExceptionHandler({ BadSqlGrammarException.class, DataAccessException.class })
    @SuppressWarnings("unused")
    public Response<Void> handleDataAccessException(Exception e) {
        Throwable root = getRootCause(e);
        String rootMsg = root != null && root.getMessage() != null ? root.getMessage() : "";

        // 常见：数据库不存在（Unknown database 'xxx'）
        if (root instanceof SQLSyntaxErrorException && rootMsg.contains("Unknown database")) {
            log.error("数据库不存在或无权限创建: {}", rootMsg);
            return ResponseUtil.fail("数据库未初始化：目标数据库不存在。请确认已创建数据库，或修改 spring.datasource.url 指向已存在的库", 503);
        }

        // 兜底：其他数据库异常
        if (root != null) {
            log.error("数据库访问异常: {}", root.getClass().getSimpleName());
        }
        return ResponseUtil.fail("数据库异常，请检查数据库连接与初始化状态", 503);
    }

    /**
     * 处理其他未预期的系统异常（兜底）
     * 直接写 JSON 到 HttpServletResponse，绕开 Spring 的内容协商。
     * 这样即使客户端 Accept 头奇怪，也能确保返回统一的 JSON 错误体，
     * 并且避免"Failure in @ExceptionHandler"导致回退到 Spring 默认白标页。
     */
    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    public void handleGeneralException(Exception e, HttpServletResponse response) {
        // 记录异常类型和消息（不记录堆栈，除非调试）
        log.error("系统异常: {}", e.getClass().getSimpleName());
        log.error("异常消息: {}", e.getMessage());

        // 直接写响应，绕过内容协商
        writeJsonResponse(response, 500, ResponseUtil.fail("当前服务暂时不可用，请稍后再试", 500));
    }

    /**
     * 处理媒体类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的媒体类型: {}", e.getContentType());
        return ResponseUtil.fail("不支持的请求格式，请使用 application/json 格式", 400);
    }

    /**
     * 处理参数类型转换失败（如 id 参数传入了非数字字符串）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        String paramName = e.getName();
        Object value = e.getValue();
        String invalidValue = value != null ? value.toString() : "null";
        Class<?> requiredType = e.getRequiredType();
        String targetType = requiredType != null ? requiredType.getSimpleName() : "unknown";
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        String queryString = request.getQueryString();

        // 详细调试日志
        log.warn("请求路径: {}", requestPath);
        log.warn("HTTP方法: {}", httpMethod);
        log.warn("查询参数: {}", queryString);
        log.warn("参数名称: {}", paramName);
        log.warn("无效值: '{}'", invalidValue);
        log.warn("期望类型: {}", targetType);

        return ResponseUtil.fail("参数格式错误，请检查请求参数", 400);
    }

    /**
     * 处理 404 资源未找到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Response<Void> handleNotFoundException(NoResourceFoundException ex) {
        log.warn("请求的资源不存在: {}", ex.getResourcePath());
        return ResponseUtil.fail("请求的资源不存在", 404);
    }

    private static Throwable getRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null && cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}