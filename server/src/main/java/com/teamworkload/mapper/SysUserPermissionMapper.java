package com.teamworkload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamworkload.entity.SysUserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserPermissionMapper extends BaseMapper<SysUserPermission> {
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
    void deleteByUserId(@Param("userId") Long userId);
}