package co.cambodia.farmer.controller;

import co.cambodia.farmer.dto.farmer.FarmerRequest;
import co.cambodia.farmer.dto.farmer.FarmerResponse;
import co.cambodia.farmer.service.FarmerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/farmers")
@RequiredArgsConstructor
public class FarmerController {

    private final FarmerService farmerService;

    @PostMapping
    public ResponseEntity<FarmerResponse> createFarmer(@Valid @RequestBody FarmerRequest request) {
        return new ResponseEntity<>(farmerService.createFarmer(request), HttpStatus.CREATED);
    }
//    @PutMapping("/{id}")
//    public ResponseEntity<FarmerResponse> updateProfileFarmer(
//            @PathVariable Long id,
//            @Valid @RequestBody FarmerRequest request) {
//
//        FarmerResponse response = farmerService.updateProfileFarmer(id, request);
//        return ResponseEntity.ok(response);
//    }
    @GetMapping("/{id}")
    public ResponseEntity<FarmerResponse> getFarmerById(@PathVariable Long id) {
        return ResponseEntity.ok(farmerService.getFarmerById(id));
    }

    @GetMapping
    public ResponseEntity<List<FarmerResponse>> getAllFarmers() {
        return ResponseEntity.ok(farmerService.getAllFarmers());
    }


    @PutMapping("/{id}")
    public ResponseEntity<FarmerResponse> updateFarmer(
            @PathVariable Long id,
            @Valid @RequestBody FarmerRequest request) {
        FarmerResponse response = farmerService.updateFarmer(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<FarmerResponse> updateProfileFarmer(
            @PathVariable Long id,
            @Valid @RequestBody FarmerRequest request) {
        FarmerResponse response = farmerService.updateProfileFarmer(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarmer(@PathVariable Long id) {
        farmerService.deleteFarmer(id);
        return ResponseEntity.noContent().build();
    }
}