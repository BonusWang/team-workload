package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.dto.ChangePasswordDTO;
import com.teamworkload.dto.LoginDTO;
import com.teamworkload.dto.TokenDTO;
import com.teamworkload.entity.SysUser;
import com.teamworkload.service.SysPermissionService;
import com.teamworkload.service.SysUserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysUserService sysUserService;
    private final SysPermissionService sysPermissionService;

    public AuthController(SysUserService sysUserService, SysPermissionService sysPermissionService) {
        this.sysUserService = sysUserService;
        this.sysPermissionService = sysPermissionService;
    }

    @PostMapping("/login")
    public Result<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        TokenDTO tokenDTO = sysUserService.login(loginDTO);
        return Result.success(tokenDTO);
    }

    @GetMapping("/info")
    public Result<Object> info() {
        Long userId = com.teamworkload.util.SecurityUtil.getCurrentUserId();
        SysUser user = sysUserService.getCurrentUser(userId);
        
        // 获取用户权限
        List<String> permissions = sysPermissionService.getUserPermissionCodes(userId);
        
        // 返回包含用户信息和权限的对象
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", permissions);
        
        return Result.success(result);
    }
    
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        sysUserService.changePassword(changePasswordDTO.getOldPassword(), changePasswordDTO.getNewPassword());
        return Result.success();
    }
}
