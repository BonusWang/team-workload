package com.teamworkload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamworkload.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    List<SysPermission> selectByRole(@Param("role") String role);
    List<SysPermission> selectByUserId(@Param("userId") Long userId);
    List<SysPermission> selectByModule(@Param("module") String module);
}