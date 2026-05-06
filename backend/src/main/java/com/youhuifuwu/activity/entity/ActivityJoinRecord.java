package com.youhuifuwu.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("activity_join_record")
public class ActivityJoinRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long activityId;

    private Long userId;

    private LocalDate participationDate;

    private LocalDateTime joinTime;

    private String status;

    private String remark;
}
