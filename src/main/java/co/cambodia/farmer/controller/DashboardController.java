package co.cambodia.farmer.controller;

import co.cambodia.farmer.service.DashboardService;
import co.cambodia.farmer.service.DashboardService.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    public ResponseEntity<DashboardSummary> getAdminDashboardMetrics() {
        return ResponseEntity.ok(dashboardService.getAdminMetrics());
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<DashboardSummary> getFarmerDashboardMetrics(@PathVariable Long farmerId) {
        return ResponseEntity.ok(dashboardService.getFarmerMetrics(farmerId));
    }
}