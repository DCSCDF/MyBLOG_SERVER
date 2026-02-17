/*
 * [GlobalExceptionHandler.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/2 18:33
 */

package com.jiuliu.myblog_dev.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.exception.BusinessException;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
    @ExceptionHandler({NotLoginException.class, NotRoleException.class, NotPermissionException.class})
    @SuppressWarnings("unused")
    public Response<Void> handleAuthException(Exception e, HttpServletRequest request) {
        log.warn("鉴权异常: {}", e.getClass().getSimpleName());

//        // 如果是OPTIONS请求且是登录异常，直接放行
//        if (e instanceof NotLoginException && "OPTIONS".equalsIgnoreCase(request.getMethod())) {
//            log.debug("OPTIONS预检请求，跳过登录检查");
//            return SaResult.ok();
//        }

        // 不记录 e.toString() 或堆栈，避免泄露内部信息
//        400: '请求参数错误',
//        401: '未授权，请重新登录',
//        403: '拒绝访问',
//        404: '请求的资源不存在', 
//        408: '请求超时',
//        429: '请求过于频繁',
//        500: '服务器内部错误',
//        502: '网关错误',
//        503: '服务不可用',
//        504: '网关超时'

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
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    @SuppressWarnings("unused")
    public Response<Void> handleRateLimitException(RateLimitException e) {
        log.warn("触发限流: {}", e.getMessage());
        return ResponseUtil.fail(e.getMessage(), 429); // HTTP 429 Too Many Requests
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

    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    public Response<Void> handleGeneralException(Exception e) {
        // 记录异常类型和消息（不记录堆栈，除非调试）
        log.error("系统异常: {}", e.getClass().getSimpleName());
        log.error("异常消息: {}", e.getMessage());

        // 返回通用错误
        return ResponseUtil.fail("当前服务暂时不可用，请稍后再试", 500);
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
     * 处理 404 资源未找到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Response<Void> handleNotFoundException(NoResourceFoundException ex) {
        log.warn("请求的资源不存在: {}", ex.getResourcePath());
        return ResponseUtil.fail("请求的资源不存在", 404);
    }
}