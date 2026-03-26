package com.smartpark.parking.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSearchResponse {

    private List<ParkingResponse> parkings;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
