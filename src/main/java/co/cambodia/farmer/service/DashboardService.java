package co.cambodia.farmer.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
@Service
public interface DashboardService {

    DashboardSummary getAdminMetrics();
    DashboardSummary getFarmerMetrics(Long farmerId);

    record DashboardSummary(
            long totalFarmers,
            long totalCustomers,
            long totalProducts,
            long totalOrders,
            BigDecimal totalRevenue,
            Map<String, Long> productCountByCategory
    ) {}
}