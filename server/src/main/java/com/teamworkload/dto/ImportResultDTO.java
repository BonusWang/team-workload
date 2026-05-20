package com.teamworkload.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportResultDTO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String batchNo;

    private List<SuccessItem> successList;

    private List<FailItem> failList;

    private List<DuplicateItem> duplicateList;

    @Data
    public static class SuccessItem {
        private Integer row;
        private String submitterName;
        private String workDate;
        private String taskNo;
    }

    @Data
    public static class FailItem {
        private Integer row;
        private String submitterName;
        private String workDate;
        private String taskNo;
        private String reason;
    }

    @Data
    public static class DuplicateItem {
        private Integer row;
        private String submitterName;
        private String workDate;
        private String taskNo;
        private Long existingId;
    }
}
