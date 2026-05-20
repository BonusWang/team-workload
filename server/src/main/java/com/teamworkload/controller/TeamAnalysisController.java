package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.service.TeamAnalysisService;
import com.teamworkload.util.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/team/analysis")
public class TeamAnalysisController {

    private final TeamAnalysisService teamAnalysisService;

    public TeamAnalysisController(TeamAnalysisService teamAnalysisService) {
        this.teamAnalysisService = teamAnalysisService;
    }

    @GetMapping("/gap")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Result<Map<String, Object>> gapAnalysis() {
        Long leaderId = SecurityUtil.getCurrentUserId();
        return Result.success(teamAnalysisService.analyzeGap(leaderId));
    }
}
