package com.youhuifuwu.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("merchant_subscription")
public class MerchantSubscription {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long merchantId;

    private LocalDateTime createdAt;
}

