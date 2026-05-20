package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamworkload.entity.Project;
import com.teamworkload.mapper.ProjectMapper;
import com.teamworkload.service.ProjectService;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
}
