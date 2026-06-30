package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.repository.CategoryRepository;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.repository.OrderRepository;
import co.cambodia.farmer.repository.ProductRepository;
import co.cambodia.farmer.repository.UserRepository;
import co.cambodia.farmer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary getAdminMetrics() {
        long farmers = farmerRepository.count();
        long customers = userRepository.count();
        long products = productRepository.count();
        long orders = orderRepository.count();

        BigDecimal revenue = BigDecimal.ZERO;

        return new DashboardSummary(farmers, customers, products, orders, revenue, new HashMap<>());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary getFarmerMetrics(Long farmerId) {
        long products = productRepository.findByFarmerId(farmerId).size();

        return new DashboardSummary(1, 0, products, 0, BigDecimal.ZERO, new HashMap<>());
    }
}