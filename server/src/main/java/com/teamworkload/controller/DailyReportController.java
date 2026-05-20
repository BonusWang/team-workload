package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.dto.DailyReportQueryDTO;
import com.teamworkload.dto.DailyReportSubmitDTO;
import com.teamworkload.dto.ImportResultDTO;
import com.teamworkload.entity.DailyReport;
import com.teamworkload.service.DailyReportService;
import com.alibaba.excel.EasyExcel;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/daily")
public class DailyReportController {

    private final DailyReportService dailyReportService;

    public DailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @PostMapping("/import")
    public Result<ImportResultDTO> importExcel(@RequestParam("file") MultipartFile file,
                                                @RequestParam(defaultValue = "QUANKAI") String source) {
        ImportResultDTO result = dailyReportService.importExcel(file, source);
        return Result.success(result);
    }

    @PostMapping("/import/confirm")
    public Result<ImportResultDTO> confirmImport(@RequestParam String batchNo) {
        ImportResultDTO result = dailyReportService.confirmImport(batchNo);
        return Result.success(result);
    }

    @GetMapping("/import/template")
    public void downloadTemplate(@RequestParam(defaultValue = "QUANKAI") String type,
                                  HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("全开系统导入模板", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream())
                .head(buildQuankaiHead())
                .sheet("全开系统导入模板")
                .doWrite(java.util.Collections.emptyList());
    }

    @GetMapping("/import/result/{batchNo}")
    public Result<ImportResultDTO> getImportResult(@PathVariable String batchNo) {
        ImportResultDTO result = dailyReportService.getImportResult(batchNo);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<DailyReport>> list(DailyReportQueryDTO query) {
        List<DailyReport> list = dailyReportService.listByQuery(query);
        return Result.success(list);
    }

    @PostMapping("/submit")
    public Result<DailyReport> submit(@RequestBody DailyReportSubmitDTO dto) {
        DailyReport saved = dailyReportService.submitReport(dto);
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<DailyReport> update(@PathVariable Long id, @RequestBody DailyReportSubmitDTO dto) {
        DailyReport updated = dailyReportService.updateReport(id, dto);
        return Result.success(updated);
    }

    private List<List<String>> buildQuankaiHead() {
        List<List<String>> head = new java.util.ArrayList<>();
        String[] headers = {
            "所属项目/产品", "模块", "系统", "需求号", "需求事项状态",
            "需求名称", "任务号", "任务名称", "任务事项类型", "测试任务类型",
            "飞书申请编号", "需求提交部门/项目申请部门", "需求提出人", "预计工时",
            "实际工时", "工作日期", "工作描述", "一级审批人", "一级审批时间",
            "二级审批人", "二级审批时间", "创建时间", "提交人", "审批状态",
            "现任级别", "所属科室", "供应商", "合同", "任务结束时间"
        };
        for (String h : headers) {
            head.add(java.util.Collections.singletonList(h));
        }
        return head;
    }
}
