package com.teamworkload.util;

import com.teamworkload.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(401, "未登录");
        }
        return (Long) authentication.getPrincipal();
    }

    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().isEmpty()) {
            throw new BusinessException(401, "未登录");
        }
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return authority.replace("ROLE_", "");
    }
}
