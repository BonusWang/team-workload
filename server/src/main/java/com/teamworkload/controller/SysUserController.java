package com.teamworkload.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamworkload.common.Result;
import com.teamworkload.dto.ResetPasswordDTO;
import com.teamworkload.dto.UserQueryDTO;
import com.teamworkload.dto.UserStatusDTO;
import com.teamworkload.entity.SysUser;
import com.teamworkload.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/user")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<SysUser>> list(@RequestParam(defaultValue = "1") Integer current,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      UserQueryDTO queryDTO) {
        Page<SysUser> page = sysUserService.listUsers(current, size, queryDTO);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getCurrentUser(id);
        return Result.success(user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@Valid @RequestBody SysUser user) {
        sysUserService.createUser(user);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        sysUserService.updateUser(id, user);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO statusDTO) {
        sysUserService.updateStatus(id, statusDTO.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordDTO resetPasswordDTO) {
        String password = resetPasswordDTO.getPassword();
        if (password == null || password.trim().isEmpty()) {
            password = "123456";
        }
        sysUserService.resetPassword(id, password);
        return Result.success();
    }

    @GetMapping("/leaders")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Result<List<SysUser>> leaders() {
        List<SysUser> leaders = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "LEADER")
                .eq(SysUser::getStatus, 1)
                .list();
        leaders.forEach(user -> user.setPassword(null));
        return Result.success(leaders);
    }
}
