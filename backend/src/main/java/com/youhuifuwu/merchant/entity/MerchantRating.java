package com.youhuifuwu.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youhuifuwu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_rating")
public class MerchantRating extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;

    private Long userId;

    private Long activityId;

    private Integer score;

    private String content;
}
