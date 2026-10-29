package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.Farmer;
import co.cambodia.farmer.dto.farmer.FarmerRequest;
import co.cambodia.farmer.dto.farmer.FarmerResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.FarmerMapper;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.service.FarmerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmerServiceImpl implements FarmerService {

    private final FarmerRepository farmerRepository;
    private final FarmerMapper farmerMapper;

    @Override
    @Transactional
    public FarmerResponse createFarmer(FarmerRequest request) {
        Farmer farmer = farmerMapper.toEntity(request);
        return farmerMapper.toResponse(farmerRepository.save(farmer));
    }

    @Override
    @Transactional(readOnly = true)
    public FarmerResponse getFarmerById(UUID id) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found with id: " + id));
        return farmerMapper.toResponse(farmer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmerResponse> getAllFarmers() {
        return farmerRepository.findAll().stream()
                .map(farmerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FarmerResponse updateFarmer(UUID id, FarmerRequest request) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found with id: " + id));

        farmer.setFullName(request.fullName());
        farmer.setPhone(request.phone());
        farmer.setProvince(request.province());
        farmer.setProfileImage(request.profileImage());

        return farmerMapper.toResponse(farmerRepository.save(farmer));
    }

    @Override
    @Transactional
    public void deleteFarmer(UUID id) {
        if (!farmerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Farmer profile not found with id: " + id);
        }
        farmerRepository.deleteById(id);
    }

    @Override
    @Transactional
    public FarmerResponse updateProfileFarmer(UUID id, FarmerRequest request) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found with id: " + id));
        farmer.setFullName(request.fullName());
        farmer.setPhone(request.phone());
        farmer.setProvince(request.province());
        farmer.setProfileImage(request.profileImage());
        Farmer updatedFarmer = farmerRepository.save(farmer);

        return mapToFarmerResponse(updatedFarmer);
    }

    private FarmerResponse mapToFarmerResponse(Farmer farmer) {
        return new FarmerResponse(
                farmer.getId(),
                farmer.getFullName(),
                farmer.getPhone(),
                farmer.getProvince(),
                farmer.getProfileImage()
        );
    }
}
