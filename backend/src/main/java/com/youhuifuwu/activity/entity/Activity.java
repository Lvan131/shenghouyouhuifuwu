package com.youhuifuwu.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youhuifuwu.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class Activity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;

    private String title;

    private String activityType;

    private String coverImage;

    private String content;

    private Integer quota;

    private Integer dailyQuota;

    private Integer joinedCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private String auditRemark;

    private Long auditedBy;

    private LocalDateTime auditedAt;
}
