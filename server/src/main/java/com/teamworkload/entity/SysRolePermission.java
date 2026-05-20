package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_role_permission")
public class SysRolePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String role;
    private String permissionCode;
    private Date createTime;
}