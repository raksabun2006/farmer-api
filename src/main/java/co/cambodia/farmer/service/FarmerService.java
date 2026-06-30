package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.farmer.FarmerRequest;
import co.cambodia.farmer.dto.farmer.FarmerResponse;
import java.util.List;

public interface FarmerService {
    FarmerResponse createFarmer(FarmerRequest request);
    FarmerResponse getFarmerById(Long id);
    List<FarmerResponse> getAllFarmers();
    FarmerResponse updateFarmer(Long id, FarmerRequest request);
    void deleteFarmer(Long id);

    FarmerResponse updateProfileFarmer(Long id, FarmerRequest request);
}