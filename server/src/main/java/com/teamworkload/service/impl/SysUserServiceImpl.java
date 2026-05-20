package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamworkload.common.BusinessException;
import com.teamworkload.dto.LoginDTO;
import com.teamworkload.dto.TokenDTO;
import com.teamworkload.dto.UserQueryDTO;
import com.teamworkload.entity.SysUser;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.service.SysUserService;
import com.teamworkload.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        SysUser user = getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        if (!loginDTO.getPassword().equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new TokenDTO(token, user.getId(), user.getUsername(), user.getName(), user.getRole());
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser getCurrentUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public Page<SysUser> listUsers(Integer current, Integer size, UserQueryDTO queryDTO) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getName())) {
            wrapper.like(SysUser::getName, queryDTO.getName());
        }
        if (StringUtils.hasText(queryDTO.getRole())) {
            wrapper.eq(SysUser::getRole, queryDTO.getRole());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getLeaderId() != null) {
            wrapper.eq(SysUser::getLeaderId, queryDTO.getLeaderId());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = page(new Page<>(current, size), wrapper);
        page.getRecords().forEach(user -> user.setPassword(null));
        return page;
    }

    @Override
    public void createUser(SysUser user) {
        SysUser existUser = getByUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(user.getPassword());
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
    }

    @Override
    public void updateUser(Long id, SysUser user) {
        SysUser existUser = getById(id);
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        user.setId(id);
        user.setPassword(null);
        user.setUsername(null);
        updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser existUser = getById(id);
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id).set(SysUser::getStatus, status);
        update(wrapper);
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser existUser = getById(id);
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id).set(SysUser::getPassword, password);
        update(wrapper);
    }

    @Override
    public List<SysUser> getSubordinates(Long leaderId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getLeaderId, leaderId)
                .eq(SysUser::getStatus, 1);
        List<SysUser> subordinates = list(wrapper);
        subordinates.forEach(user -> user.setPassword(null));
        return subordinates;
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = com.teamworkload.util.SecurityUtil.getCurrentUserId();
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!oldPassword.equals(user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId).set(SysUser::getPassword, newPassword);
        update(wrapper);
    }
}
