package com.teamworkload.controller;

import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamworkload.common.Result;
import com.teamworkload.entity.Project;
import com.teamworkload.service.ProjectService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/page")
    public Result<Page<Project>> page(@RequestParam(defaultValue = "1") Integer current,
                                      @RequestParam(defaultValue = "10") Integer size) {
        Page<Project> page = projectService.page(new Page<>(current, size));
        return Result.success(page);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Project project) {
        projectService.save(project);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Project>> list() {
        List<Project> list = projectService.list();
        return Result.success(list);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Project project) {
        projectService.updateById(project);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return Result.success();
    }
}
