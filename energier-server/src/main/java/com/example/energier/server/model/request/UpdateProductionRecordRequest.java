package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Edits one stored production record. Send the fields you want to change. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductionRecordRequest {
    private Double quantity;
    private String quality;
    private String date;
    private String time;
}
