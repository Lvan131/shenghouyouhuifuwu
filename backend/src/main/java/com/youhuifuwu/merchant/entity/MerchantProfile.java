package com.youhuifuwu.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youhuifuwu.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_profile")
public class MerchantProfile extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private String merchantName;

    private String merchantType;

    private String contactName;

    private String contactPhone;

    private String address;

    private BigDecimal distanceKm;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String description;

    private Integer status;
}
