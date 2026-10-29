package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.farmer.FarmerRequest;
import co.cambodia.farmer.dto.farmer.FarmerResponse;
import java.util.List;
import java.util.UUID;

public interface FarmerService {
    FarmerResponse createFarmer(FarmerRequest request);
    FarmerResponse getFarmerById(UUID id);
    List<FarmerResponse> getAllFarmers();
    FarmerResponse updateFarmer(UUID id, FarmerRequest request);
    void deleteFarmer(UUID id);

    FarmerResponse updateProfileFarmer(UUID id, FarmerRequest request);
}
