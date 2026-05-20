package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_user_permission")
public class SysUserPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String permissionCode;
    private Date createTime;
}