package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductionRecordRequest {
    /** yyyy-MM-dd */
    private String date;
    /** HH:mm (optional) */
    private String time;
    private Double quantity;
    private String quality;
}
