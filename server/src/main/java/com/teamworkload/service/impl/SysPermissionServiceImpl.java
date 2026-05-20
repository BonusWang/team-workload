package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamworkload.entity.SysPermission;
import com.teamworkload.entity.SysRolePermission;
import com.teamworkload.entity.SysUser;
import com.teamworkload.entity.SysUserPermission;
import com.teamworkload.mapper.SysPermissionMapper;
import com.teamworkload.mapper.SysRolePermissionMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.mapper.SysUserPermissionMapper;
import com.teamworkload.service.SysPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;
    private final SysUserMapper sysUserMapper;

    public SysPermissionServiceImpl(SysPermissionMapper sysPermissionMapper, SysRolePermissionMapper sysRolePermissionMapper, SysUserPermissionMapper sysUserPermissionMapper, SysUserMapper sysUserMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysUserPermissionMapper = sysUserPermissionMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<SysPermission> getAllPermissions() {
        return sysPermissionMapper.selectList(null);
    }

    @Override
    public List<SysPermission> getPermissionsByModule(String module) {
        QueryWrapper<SysPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("module", module);
        return sysPermissionMapper.selectList(wrapper);
    }

    @Override
    public List<SysPermission> getRolePermissions(String role) {
        return sysPermissionMapper.selectByRole(role);
    }

    @Override
    public List<SysPermission> getUserPermissions(Long userId) {
        return sysPermissionMapper.selectByUserId(userId);
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        // 获取用户所属角色的权限
        SysUser currentUser = sysUserMapper.selectById(userId);
        List<String> rolePermissions = sysRolePermissionMapper.selectPermissionCodesByRole(currentUser.getRole());
        
        // 获取用户个人的权限配置（包括正向和负向）
        List<String> userPermissionConfigs = sysUserPermissionMapper.selectPermissionCodesByUserId(userId);
        
        // 处理用户权限配置：
        // 1. 以角色默认权限为基础
        // 2. 如果用户权限配置中包含以"-"开头的权限代码，则移除该权限
        // 3. 如果用户权限配置中包含普通权限代码，则添加该权限
        Set<String> finalPermissions = new HashSet<>(rolePermissions);
        
        for (String config : userPermissionConfigs) {
            if (config.startsWith("-")) {
                // 负向权限配置：移除该权限
                String permissionCode = config.substring(1);
                finalPermissions.remove(permissionCode);
            } else {
                // 正向权限配置：添加该权限
                finalPermissions.add(config);
            }
        }
        
        return new ArrayList<>(finalPermissions);
    }

    @Transactional
    @Override
    public void assignUserPermissions(Long userId, List<String> permissionCodes) {
        // 获取用户所属角色的权限
        SysUser currentUser = sysUserMapper.selectById(userId);
        List<String> rolePermissions = sysRolePermissionMapper.selectPermissionCodesByRole(currentUser.getRole());
        
        // 计算需要保存的用户权限配置
        List<String> userPermissions = new ArrayList<>();
        
        // 获取所有系统权限
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        List<String> allPermissionCodes = allPermissions.stream().map(SysPermission::getCode).collect(Collectors.toList());
        
        if (permissionCodes != null) {
            for (String permissionCode : allPermissionCodes) {
                boolean userHasPermission = permissionCodes.contains(permissionCode);
                boolean roleHasPermission = rolePermissions.contains(permissionCode);
                
                if (userHasPermission && !roleHasPermission) {
                    // 用户有此权限，但角色没有：需要添加正向配置
                    userPermissions.add(permissionCode);
                } else if (!userHasPermission && roleHasPermission) {
                    // 用户没有此权限，但角色有：需要添加负向配置
                    userPermissions.add("-" + permissionCode);
                }
                // 如果权限状态与角色默认一致，则不需要保存配置
            }
        }
        
        // 删除用户现有的权限配置
        sysUserPermissionMapper.deleteByUserId(userId);
        
        // 添加新的权限配置
        for (String permissionConfig : userPermissions) {
            SysUserPermission userPermission = new SysUserPermission();
            userPermission.setUserId(userId);
            userPermission.setPermissionCode(permissionConfig);
            sysUserPermissionMapper.insert(userPermission);
        }
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        List<String> userPermissions = getUserPermissionCodes(userId);
        return userPermissions.contains(permissionCode);
    }
}