package com.youhuifuwu.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class ActivitySaveRequest {

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "activityType cannot be blank")
    private String activityType;

    private String coverImage;

    @NotBlank(message = "content cannot be blank")
    private String content;

    private Integer quota;

    @NotNull(message = "dailyQuota cannot be null")
    @Positive(message = "dailyQuota must be greater than 0")
    private Integer dailyQuota;

    @NotNull(message = "startTime cannot be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "endTime cannot be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
