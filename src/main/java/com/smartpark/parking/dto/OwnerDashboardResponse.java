package com.smartpark.parking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardResponse {

    private Long parkingId;
    private String parkingName;
    private Integer totalSlots;
    private Integer availableSlots;
    private Integer upcomingBookingsCount;
    private Integer completedBookingsCount;
    private Integer cancelledBookingsCount;
    private BigDecimal utilizationPercent;
    private BigDecimal grossEarnings;
    private BigDecimal fees;
    private BigDecimal netEarnings;
    private List<BookingSummaryDto> recentBookings;
}
