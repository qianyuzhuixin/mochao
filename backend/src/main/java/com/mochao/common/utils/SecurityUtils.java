package com.mochao.common.utils;

import com.mochao.common.constant.Constants;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 判断是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long;
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        return isAdmin() ? Constants.ROLE_ADMIN : Constants.ROLE_USER;
    }
}
