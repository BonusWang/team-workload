package com.teamworkload.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamworkload.common.BusinessException;
import com.teamworkload.dto.*;
import com.teamworkload.entity.DailyReport;
import com.teamworkload.entity.SysUser;
import com.teamworkload.mapper.DailyReportMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.service.DailyReportService;
import com.teamworkload.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DailyReportServiceImpl extends ServiceImpl<DailyReportMapper, DailyReport> implements DailyReportService {

    private final SysUserMapper sysUserMapper;
    private static final Map<String, ImportResultDTO> importCache = new ConcurrentHashMap<>();

    public DailyReportServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public ImportResultDTO importExcel(MultipartFile file, String source) {
        String batchNo = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ImportResultDTO result = new ImportResultDTO();
        result.setBatchNo(batchNo);
        result.setSuccessList(new ArrayList<>());
        result.setFailList(new ArrayList<>());
        result.setDuplicateList(new ArrayList<>());

        List<DailyReport> toSave = new ArrayList<>();
        Map<String, SysUser> userCache = new HashMap<>();

        try {
            if ("QUANKAI".equals(source)) {
                EasyExcel.read(file.getInputStream(), QuankaiImportDTO.class, new AnalysisEventListener<QuankaiImportDTO>() {
                    int row = 1;

                    @Override
                    public void invoke(QuankaiImportDTO data, AnalysisContext context) {
                        row++;
                        processRow(data.getProjectName(), data.getTaskNo(), data.getTaskName(),
                                data.getWorkDateStr(), data.getHoursStr(), data.getWorkDescription(),
                                data.getSubmitterName(),
                                row, source, batchNo, toSave, result, userCache);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                    }
                }).sheet().doRead();
            } else {
                EasyExcel.read(file.getInputStream(), DailyReportImportDTO.class, new AnalysisEventListener<DailyReportImportDTO>() {
                    int row = 1;

                    @Override
                    public void invoke(DailyReportImportDTO data, AnalysisContext context) {
                        row++;
                        processRow(data.getProjectName(), data.getTaskNo(), data.getTaskName(),
                                data.getWorkDateStr(), data.getHoursStr(), data.getWorkDescription(),
                                data.getSubmitterName(),
                                row, source, batchNo, toSave, result, userCache);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                    }
                }).sheet().doRead();
            }
        } catch (IOException e) {
            throw new BusinessException("读取Excel文件失败: " + e.getMessage());
        }

        result.setTotalCount(result.getSuccessList().size() + result.getFailList().size() + result.getDuplicateList().size());
        result.setSuccessCount(result.getSuccessList().size());
        result.setFailCount(result.getFailList().size());

        if (!toSave.isEmpty()) {
            saveBatch(toSave);
        }

        importCache.put(batchNo, result);
        return result;
    }

    private void processRow(String projectName, String taskNo, String taskName, String workDateStr,
                            String hoursStr, String workDescription, String submitterName, int row, String source, String batchNo,
                            List<DailyReport> toSave, ImportResultDTO result, Map<String, SysUser> userCache) {
        if (projectName == null || projectName.trim().isEmpty()) {
            addFail(result, row, submitterName, workDateStr, taskNo, "所属项目/产品不能为空");
            return;
        }
        if (taskNo == null || taskNo.trim().isEmpty()) {
            addFail(result, row, submitterName, workDateStr, taskNo, "任务号不能为空");
            return;
        }
        if (submitterName == null || submitterName.trim().isEmpty()) {
            addFail(result, row, submitterName, workDateStr, taskNo, "提交人不能为空");
            return;
        }

        LocalDate workDate;
        try {
            workDate = parseDate(workDateStr);
        } catch (Exception e) {
            addFail(result, row, submitterName, workDateStr, taskNo, "日期格式错误: " + workDateStr);
            return;
        }

        BigDecimal hours;
        try {
            hours = new BigDecimal(hoursStr);
            if (hours.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(new BigDecimal("24")) > 0) {
                addFail(result, row, submitterName, workDateStr, taskNo, "工时范围应为0-24小时");
                return;
            }
        } catch (Exception e) {
            addFail(result, row, submitterName, workDateStr, taskNo, "工时格式错误: " + hoursStr);
            return;
        }

        SysUser user = userCache.computeIfAbsent(submitterName.trim(), name -> {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getName, name);
            return sysUserMapper.selectOne(wrapper);
        });

        if (user == null) {
            addFail(result, row, submitterName, workDateStr, taskNo, "提交人不存在: " + submitterName);
            return;
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            addFail(result, row, submitterName, workDateStr, taskNo, "提交人已被禁用: " + submitterName);
            return;
        }

        LambdaQueryWrapper<DailyReport> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(DailyReport::getUserId, user.getId())
                .eq(DailyReport::getWorkDate, workDate)
                .eq(DailyReport::getTaskNo, taskNo.trim());
        DailyReport existing = baseMapper.selectOne(dupWrapper);

        if (existing != null) {
            ImportResultDTO.DuplicateItem dup = new ImportResultDTO.DuplicateItem();
            dup.setRow(row);
            dup.setSubmitterName(submitterName);
            dup.setWorkDate(workDate.toString());
            dup.setTaskNo(taskNo);
            dup.setExistingId(existing.getId());
            result.getDuplicateList().add(dup);
            return;
        }

        DailyReport report = new DailyReport();
        report.setUserId(user.getId());
        report.setWorkDate(workDate);
        report.setProjectName(projectName.trim());
        report.setTaskNo(taskNo.trim());
        report.setTaskName(taskName != null ? taskName.trim() : "");
        report.setHours(hours);
        report.setWorkDescription(workDescription != null ? workDescription.trim() : "");
        report.setSource(source);
        report.setImportBatchNo(batchNo);
        toSave.add(report);

        ImportResultDTO.SuccessItem success = new ImportResultDTO.SuccessItem();
        success.setRow(row);
        success.setSubmitterName(submitterName);
        success.setWorkDate(workDate.toString());
        success.setTaskNo(taskNo);
        result.getSuccessList().add(success);
    }

    private void addFail(ImportResultDTO result, int row, String submitterName, String workDate, String taskNo, String reason) {
        ImportResultDTO.FailItem fail = new ImportResultDTO.FailItem();
        fail.setRow(row);
        fail.setSubmitterName(submitterName);
        fail.setWorkDate(workDate);
        fail.setTaskNo(taskNo);
        fail.setReason(reason);
        result.getFailList().add(fail);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("日期为空");
        }
        dateStr = dateStr.trim();
        if (dateStr.contains("年")) {
            dateStr = dateStr.replace("年", "-").replace("月", "-").replace("日", "");
        }
        String[] patterns = {"yyyy-MM-dd", "yyyy/M/d", "yyyy/M/dd", "yyyy/MM/d", "yyyy/MM/dd",
                "M/d/yyyy", "yyyy/M/d H:mm", "yyyy-MM-dd HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {
        }
        return LocalDate.parse(dateStr);
    }

    @Override
    @Transactional
    public ImportResultDTO confirmImport(String batchNo) {
        ImportResultDTO result = importCache.get(batchNo);
        if (result == null) {
            throw new BusinessException("导入批次不存在或已过期");
        }

        List<DailyReport> toSave = new ArrayList<>();
        for (ImportResultDTO.DuplicateItem dup : result.getDuplicateList()) {
            LambdaQueryWrapper<DailyReport> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(DailyReport::getId, dup.getExistingId());
            remove(delWrapper);

            DailyReport report = new DailyReport();
            report.setUserId(getUserIdByName(dup.getSubmitterName()));
            report.setWorkDate(LocalDate.parse(dup.getWorkDate()));
            report.setTaskNo(dup.getTaskNo());
            report.setProjectName("");
            report.setTaskName("");
            report.setHours(BigDecimal.ZERO);
            report.setSource("QUANKAI");
            report.setImportBatchNo(batchNo + "-confirm");
            toSave.add(report);
        }

        if (!toSave.isEmpty()) {
            saveBatch(toSave);
        }

        result.getSuccessList().addAll(convertDuplicatesToSuccess(result.getDuplicateList()));
        result.setSuccessCount(result.getSuccessList().size());
        result.getDuplicateList().clear();
        importCache.put(batchNo, result);
        return result;
    }

    private Long getUserIdByName(String name) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getName, name);
        SysUser user = sysUserMapper.selectOne(wrapper);
        return user != null ? user.getId() : null;
    }

    private List<ImportResultDTO.SuccessItem> convertDuplicatesToSuccess(List<ImportResultDTO.DuplicateItem> duplicates) {
        List<ImportResultDTO.SuccessItem> list = new ArrayList<>();
        for (ImportResultDTO.DuplicateItem dup : duplicates) {
            ImportResultDTO.SuccessItem success = new ImportResultDTO.SuccessItem();
            success.setRow(dup.getRow());
            success.setSubmitterName(dup.getSubmitterName());
            success.setWorkDate(dup.getWorkDate());
            success.setTaskNo(dup.getTaskNo());
            list.add(success);
        }
        return list;
    }

    @Override
    public ImportResultDTO getImportResult(String batchNo) {
        return importCache.get(batchNo);
    }

    @Override
    public List<DailyReport> listByQuery(DailyReportQueryDTO query) {
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            wrapper.eq(DailyReport::getUserId, query.getUserId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(DailyReport::getWorkDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(DailyReport::getWorkDate, query.getEndDate());
        }
        if (query.getProjectName() != null && !query.getProjectName().isEmpty()) {
            wrapper.like(DailyReport::getProjectName, query.getProjectName());
        }
        if (query.getSource() != null && !query.getSource().isEmpty()) {
            wrapper.eq(DailyReport::getSource, query.getSource());
        }
        wrapper.orderByDesc(DailyReport::getWorkDate);
        return list(wrapper);
    }

    @Override
    @Transactional
    public DailyReport submitReport(DailyReportSubmitDTO dto) {
        if (dto.getProjectName() == null || dto.getProjectName().trim().isEmpty()) {
            throw new BusinessException("所属项目/产品不能为空");
        }
        if (dto.getTaskName() == null || dto.getTaskName().trim().isEmpty()) {
            throw new BusinessException("任务名称不能为空");
        }
        if (dto.getWorkDate() == null || dto.getWorkDate().trim().isEmpty()) {
            throw new BusinessException("工作日期不能为空");
        }
        if (dto.getHours() == null || dto.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("实际工时必须大于0");
        }
        if (dto.getWorkDescription() == null || dto.getWorkDescription().trim().isEmpty()) {
            throw new BusinessException("工作描述不能为空");
        }

        // 获取当前登录用户
        Long currentUserId = SecurityUtil.getCurrentUserId();
        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("当前用户已被禁用");
        }

        LocalDate workDate;
        try {
            workDate = parseDate(dto.getWorkDate());
        } catch (Exception e) {
            throw new BusinessException("日期格式错误: " + dto.getWorkDate());
        }

        DailyReport report = new DailyReport();
        report.setUserId(user.getId());
        report.setWorkDate(workDate);
        report.setProjectName(dto.getProjectName().trim());
        report.setTaskNo(dto.getTaskNo() != null ? dto.getTaskNo().trim() : "");
        report.setTaskName(dto.getTaskName().trim());
        report.setHours(dto.getHours());
        report.setWorkDescription(dto.getWorkDescription().trim());
        report.setSource("OTHER");
        save(report);
        return report;
    }
    @Override
    @Transactional
    public DailyReport updateReport(Long id, DailyReportSubmitDTO dto) {
        DailyReport report = getById(id);
        if (report == null) {
            throw new BusinessException("日报记录不存在");
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!report.getUserId().equals(currentUserId)) {
            throw new BusinessException("只能编辑自己的日报记录");
        }

        if (dto.getProjectName() == null || dto.getProjectName().trim().isEmpty()) {
            throw new BusinessException("所属项目/产品不能为空");
        }
        if (dto.getTaskName() == null || dto.getTaskName().trim().isEmpty()) {
            throw new BusinessException("任务名称不能为空");
        }
        if (dto.getWorkDate() == null || dto.getWorkDate().trim().isEmpty()) {
            throw new BusinessException("工作日期不能为空");
        }
        if (dto.getHours() == null || dto.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("实际工时必须大于0");
        }
        if (dto.getWorkDescription() == null || dto.getWorkDescription().trim().isEmpty()) {
            throw new BusinessException("工作描述不能为空");
        }

        LocalDate workDate;
        try {
            workDate = parseDate(dto.getWorkDate());
        } catch (Exception e) {
            throw new BusinessException("日期格式错误: " + dto.getWorkDate());
        }

        report.setWorkDate(workDate);
        report.setProjectName(dto.getProjectName().trim());
        report.setTaskNo(dto.getTaskNo() != null ? dto.getTaskNo().trim() : "");
        report.setTaskName(dto.getTaskName().trim());
        report.setHours(dto.getHours());
        report.setWorkDescription(dto.getWorkDescription().trim());
        updateById(report);
        return report;
    }
}
