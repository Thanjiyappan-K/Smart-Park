package com.smartpark.parking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryDto {
    private Long bookingId;
    private String driverName;
    private String startTime;
    private String endTime;
    private String status;
    private BigDecimal amount;
}
