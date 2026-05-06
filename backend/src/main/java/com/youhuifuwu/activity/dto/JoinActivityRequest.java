package com.youhuifuwu.activity.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class JoinActivityRequest {

    @NotNull(message = "participationDate cannot be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate participationDate;
}
