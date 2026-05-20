package com.teamworkload.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamworkload.dto.DailyReportQueryDTO;
import com.teamworkload.dto.DailyReportSubmitDTO;
import com.teamworkload.dto.ImportResultDTO;
import com.teamworkload.entity.DailyReport;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DailyReportService extends IService<DailyReport> {

    ImportResultDTO importExcel(MultipartFile file, String source);

    ImportResultDTO confirmImport(String batchNo);

    ImportResultDTO getImportResult(String batchNo);

    List<DailyReport> listByQuery(DailyReportQueryDTO query);

    DailyReport submitReport(DailyReportSubmitDTO dto);

    DailyReport updateReport(Long id, DailyReportSubmitDTO dto);
}
