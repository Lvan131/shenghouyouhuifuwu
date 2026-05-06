package com.youhuifuwu.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivityAuditRequest {

    @NotBlank(message = "auditResult cannot be blank")
    private String auditResult;

    private String auditRemark;
}

