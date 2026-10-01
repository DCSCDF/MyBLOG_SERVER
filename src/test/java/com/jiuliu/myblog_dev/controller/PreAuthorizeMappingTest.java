package com.jiuliu.myblog_dev.controller;

import com.jiuliu.myblog_dev.controller.blog.BlogController;
import com.jiuliu.myblog_dev.controller.blog.GlobalArticleController;
import com.jiuliu.myblog_dev.controller.blog.categories.CategoryController;
import com.jiuliu.myblog_dev.controller.comment.CommentController;
import com.jiuliu.myblog_dev.controller.comment.GlobalCommentController;
import com.jiuliu.myblog_dev.controller.config.ConfigController;
import com.jiuliu.myblog_dev.controller.link.FriendLinkController;
import com.jiuliu.myblog_dev.controller.mail.MailController;
import com.jiuliu.myblog_dev.controller.monitor.MemoryMonitorController;
import com.jiuliu.myblog_dev.controller.oss.GlobalOssController;
import com.jiuliu.myblog_dev.controller.oss.OssController;
import com.jiuliu.myblog_dev.controller.seo.SeoController;
import com.jiuliu.myblog_dev.controller.user.UserController;
import com.jiuliu.myblog_dev.controller.user.permission.PermissionController;
import com.jiuliu.myblog_dev.controller.user.permissionGroup.PermissionGroupController;
import com.jiuliu.myblog_dev.controller.user.role.RoleController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the migrated permission annotations: every controller method that
 * was permission-checked before the Spring Security migration must carry the
 * equivalent @PreAuthorize with the identical permission code, and the total
 * count of checks must not silently drop.
 */
class PreAuthorizeMappingTest {

    private static final List<Class<?>> PROTECTED_CONTROLLERS = List.of(
            BlogController.class, GlobalArticleController.class, CategoryController.class,
            CommentController.class, GlobalCommentController.class, ConfigController.class,
            FriendLinkController.class, MailController.class, MemoryMonitorController.class,
            GlobalOssController.class, OssController.class, SeoController.class,
            UserController.class, PermissionController.class, PermissionGroupController.class,
            RoleController.class);

    private String preAuthorizeValue(Class<?> type, String methodName) throws Exception {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertNotNull(annotation, type.getSimpleName() + "." + methodName + " must be @PreAuthorize");
                return annotation.value();
            }
        }
        throw new AssertionError("method not found: " + type.getSimpleName() + "." + methodName);
    }

    @Test
    void representativePermissionsKeepIdenticalCodes() throws Exception {
        assertEquals("hasAuthority('article:create')", preAuthorizeValue(BlogController.class, "createBlog"));
        assertEquals("hasAuthority('article:delete')", preAuthorizeValue(BlogController.class, "deleteBlog"));
        assertEquals("hasAuthority('category:create')", preAuthorizeValue(CategoryController.class, "createCategory"));
        assertEquals("hasAuthority('system:user:delete')", preAuthorizeValue(UserController.class, "deleteUser"));
        assertEquals("hasAuthority('system:role:create')", preAuthorizeValue(RoleController.class, "createRole"));
        assertEquals("hasAuthority('system:permission')", preAuthorizeValue(PermissionController.class, "getPagePermissions"));
        assertEquals("hasAuthority('oss:create')", preAuthorizeValue(OssController.class, "uploadImage"));
        assertEquals("hasRole('SUPER_ADMIN')", preAuthorizeValue(MemoryMonitorController.class, "getMemoryStatus"));
    }

    @Test
    void everyProtectedControllerStillHasChecks() {
        for (Class<?> controller : PROTECTED_CONTROLLERS) {
            long checks = Arrays.stream(controller.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PreAuthorize.class))
                    .count();
            assertTrue(checks > 0, controller.getSimpleName() + " lost all @PreAuthorize checks");
        }
    }

    @Test
    void totalCheckCountMatchesMigratedBaseline() {
        long total = PROTECTED_CONTROLLERS.stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(PreAuthorize.class))
                .count();
        // 70 permission checks + 2 role checks across the protected controllers
        // (the one commented-out oss:delete check is not a real annotation)
        assertEquals(72, total);
    }
}
