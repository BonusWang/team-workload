package com.teamworkload.config;

import com.teamworkload.service.SysPermissionService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class PermissionEvaluatorConfig implements PermissionEvaluator {

    private final SysPermissionService sysPermissionService;

    public PermissionEvaluatorConfig(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(permission instanceof String)) {
            return false;
        }
        
        String permissionCode = (String) permission;
        Long userId = getCurrentUserId(authentication);
        
        return sysPermissionService.hasPermission(userId, permissionCode);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !(permission instanceof String)) {
            return false;
        }
        
        String permissionCode = (String) permission;
        Long userId = getCurrentUserId(authentication);
        
        return sysPermissionService.hasPermission(userId, permissionCode);
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        } else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return Long.parseLong(userDetails.getUsername());
        }
        return null;
    }
}