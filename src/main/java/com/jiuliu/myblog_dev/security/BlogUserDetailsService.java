package com.jiuliu.myblog_dev.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.service.authentication.AuthenticationService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link BlogUserDetails} from {@code sys_user} plus RBAC authorities.
 * Authorities are resolved fresh on every request so role/permission edits
 * and account disabling take effect immediately, matching the behavior the
 * ACL has always exposed.
 */
@Service
public class BlogUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final AuthenticationService authenticationService;

    public BlogUserDetailsService(SysUserMapper sysUserMapper,
                                  AuthenticationService authenticationService) {
        this.sysUserMapper = sysUserMapper;
        this.authenticationService = authenticationService;
    }

    @Override
    public BlogUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username)
                .eq("is_deleted", 0));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return toUserDetails(user);
    }

    /** Load by primary key (the JWT subject). Returns null when missing/deleted. */
    public BlogUserDetails loadUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            return null;
        }
        return toUserDetails(user);
    }

    private BlogUserDetails toUserDetails(SysUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : authenticationService.getRoleList(user.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : authenticationService.getPermissionList(user.getId())) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new BlogUserDetails(user.getId(), user.getUsername(), user.getPassword(),
                enabled, authorities);
    }
}
