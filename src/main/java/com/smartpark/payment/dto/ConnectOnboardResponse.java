package com.smartpark.payment.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectOnboardResponse {

    private String url;
    private String accountId;
}
