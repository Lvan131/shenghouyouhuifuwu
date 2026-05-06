package com.youhuifuwu.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("activity_audit_record")
public class ActivityAuditRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long activityId;

    private Long auditorId;

    private String auditResult;

    private String auditRemark;

    private LocalDateTime createdAt;
}

