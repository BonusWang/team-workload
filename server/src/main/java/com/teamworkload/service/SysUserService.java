package com.teamworkload.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.teamworkload.dto.LoginDTO;
import com.teamworkload.dto.TokenDTO;
import com.teamworkload.dto.UserQueryDTO;
import com.teamworkload.entity.SysUser;
import java.util.List;

public interface SysUserService extends IService<SysUser> {

    TokenDTO login(LoginDTO loginDTO);

    SysUser getByUsername(String username);

    SysUser getCurrentUser(Long userId);

    Page<SysUser> listUsers(Integer current, Integer size, UserQueryDTO queryDTO);

    void createUser(SysUser user);

    void updateUser(Long id, SysUser user);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String password);
    
    List<SysUser> getSubordinates(Long leaderId);
    
    void changePassword(String oldPassword, String newPassword);
}
