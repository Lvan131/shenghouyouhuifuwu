package com.youhuifuwu.merchant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MerchantProfileUpdateRequest {

    @NotBlank(message = "Username is required")
    private String username;

    private String password;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotBlank(message = "Merchant type is required")
    private String merchantType;

    private String contactName;

    private String contactPhone;

    @NotBlank(message = "Address is required")
    private String address;

    @Digits(integer = 6, fraction = 2, message = "Distance format is invalid")
    @DecimalMin(value = "0.00", message = "Distance must be greater than or equal to 0")
    private BigDecimal distanceKm;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String description;
}
