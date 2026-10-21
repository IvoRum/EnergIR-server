package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductionRecordResponse {
    private Long id;
    /** yyyy-MM-dd */
    private String date;
    /** HH:mm */
    private String time;
    private double quantity;
    private String quality;
}
