package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Renames / restyles a water group. Send the fields you want to change. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWaterGroupRequest {
    private String name;
    private String description;
    private String color;
}
