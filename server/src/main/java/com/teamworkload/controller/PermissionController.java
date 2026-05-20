package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.entity.SysPermission;
import com.teamworkload.service.SysPermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    private final SysPermissionService sysPermissionService;

    public PermissionController(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysPermission>> getAllPermissions() {
        return Result.success(sysPermissionService.getAllPermissions());
    }

    @GetMapping("/module/{module}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysPermission>> getPermissionsByModule(@PathVariable String module) {
        return Result.success(sysPermissionService.getPermissionsByModule(module));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysPermission>> getRolePermissions(@PathVariable String role) {
        return Result.success(sysPermissionService.getRolePermissions(role));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysPermission>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(sysPermissionService.getUserPermissions(userId));
    }

    @GetMapping("/user/{userId}/codes")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<String>> getUserPermissionCodes(@PathVariable Long userId) {
        return Result.success(sysPermissionService.getUserPermissionCodes(userId));
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignUserPermissions(@PathVariable Long userId, @RequestBody List<String> permissionCodes) {
        sysPermissionService.assignUserPermissions(userId, permissionCodes);
        return Result.success();
    }

    @GetMapping("/check/{userId}/{permissionCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> checkPermission(@PathVariable Long userId, @PathVariable String permissionCode) {
        return Result.success(sysPermissionService.hasPermission(userId, permissionCode));
    }
}