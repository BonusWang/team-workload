package com.teamworkload.service;

import com.teamworkload.entity.SysPermission;
import org.springframework.stereotype.Service;

import java.util.List;

public interface SysPermissionService {
    List<SysPermission> getAllPermissions();
    List<SysPermission> getPermissionsByModule(String module);
    List<SysPermission> getRolePermissions(String role);
    List<SysPermission> getUserPermissions(Long userId);
    List<String> getUserPermissionCodes(Long userId);
    void assignUserPermissions(Long userId, List<String> permissionCodes);
    boolean hasPermission(Long userId, String permissionCode);
}