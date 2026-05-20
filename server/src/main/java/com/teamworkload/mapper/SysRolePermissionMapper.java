package com.teamworkload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamworkload.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
    List<String> selectPermissionCodesByRole(@Param("role") String role);
    void deleteByRole(@Param("role") String role);
}