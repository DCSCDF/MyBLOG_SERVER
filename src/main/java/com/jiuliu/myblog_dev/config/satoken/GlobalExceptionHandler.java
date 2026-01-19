package com.jiuliu.myblog_dev.config.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public SaResult handleValidationException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldError();
        String message = (firstError != null) ? firstError.getDefaultMessage() : "请求参数格式错误";
        log.warn("参数校验失败: {}", message);
        return SaResult.error(message).setCode(400);
    }

    /**
     * 处理业务逻辑中的非法参数（如密码错误、用户不存在等）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @SuppressWarnings("unused")
    public SaResult handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("业务参数错误: {}", e.getMessage());
        return SaResult.error(e.getMessage()).setCode(400);
    }

    /**
     * 处理 Sa-Token 鉴权异常
     */
    @ExceptionHandler({NotLoginException.class, NotRoleException.class, NotPermissionException.class})
    @SuppressWarnings("unused")
    public SaResult handleAuthException(Exception e) {
        log.warn("鉴权异常: {}", e.getClass().getSimpleName());
        // 不记录 e.toString() 或堆栈，避免泄露内部信息

        if (e instanceof NotLoginException) {
            return SaResult.error("未登录，请先登录").setCode(401);
        } else if (e instanceof NotRoleException) {
            return SaResult.error("没有角色权限").setCode(403);
        } else if (e instanceof NotPermissionException) {
            return SaResult.error("没有权限").setCode(403);
        }
        return SaResult.error("鉴权失败").setCode(403);
    }
    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    @SuppressWarnings("unused")
    public SaResult handleRateLimitException(RateLimitException e) {
        log.warn("触发限流: {}", e.getMessage());
        return SaResult.error(e.getMessage()).setCode(429); // HTTP 429 Too Many Requests
    }
    /**
     * 处理其他未预期的系统异常
     */
    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    public SaResult handleGeneralException(Exception e) {
        // 记录异常类型和消息（不记录堆栈，除非调试）
        log.error("系统异常: {}", e.getClass().getSimpleName());
        log.error("异常消息: {}", e.getMessage());

        // 返回通用错误
        return SaResult.error("当前服务暂时不可用，请稍后再试").setCode(500);
    }
}