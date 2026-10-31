package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A manually entered dashboard reading for an electric meter. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMeterReadingRequest {
    private Double value;
    /** kWh | MWh | Wh — converted to kWh on the server. */
    private String unit;
    private Instant createdOn;
}
