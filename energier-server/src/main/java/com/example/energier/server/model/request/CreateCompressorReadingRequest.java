package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Manual air-output entry: cubic metres on a date (back-dating allowed). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCompressorReadingRequest {
    private Double quantity;
    private String date;
    private String time;
}
