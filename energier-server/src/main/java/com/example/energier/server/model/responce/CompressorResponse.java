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
public class CompressorResponse {
    private Integer id;
    private boolean archived;
    private String name;
    private String description;
    /** Null when the compressor has no electric meter attached. */
    private Integer electricMeterId;
    private String electricMeterName;
    /** Cubic metres over all readings. */
    private double totalProduced;
    /** Cubic metres per day, averaged over the days with readings. */
    private double dailyAverage;
    private String createdAt;
    private String lastUpdated;
    private List<CompressorReadingResponse> readings;
}
