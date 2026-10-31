package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer groupId;
    private String unit;
    private double totalProduced;
    private double dailyAverage;
    /** yyyy-MM-dd */
    private String createdAt;
    /** ISO-8601 */
    private String lastUpdated;
    private List<ProductionRecordResponse> productionData;
}
