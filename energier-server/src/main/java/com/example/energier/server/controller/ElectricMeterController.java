package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateElectricMeterRequest;
import com.example.energier.server.model.request.UpdateElectricMeterRequest;
import com.example.energier.server.model.responce.ElectricMeterResponse;
import com.example.energier.server.service.ElectricMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meters")
@RequiredArgsConstructor
public class ElectricMeterController {

    private final ElectricMeterService electricMeterService;

    @GetMapping
    public ResponseEntity<List<ElectricMeterResponse>> getAll() {
        return ResponseEntity.ok(electricMeterService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ElectricMeterResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(electricMeterService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ElectricMeterResponse> create(@RequestBody CreateElectricMeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(electricMeterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ElectricMeterResponse> update(
            @PathVariable Integer id,
            @RequestBody UpdateElectricMeterRequest request
    ) {
        return ResponseEntity.ok(electricMeterService.update(id, request));
    }
}
