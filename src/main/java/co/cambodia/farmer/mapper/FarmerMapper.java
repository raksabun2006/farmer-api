package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.Farmer;
import co.cambodia.farmer.dto.farmer.FarmerRequest;
import co.cambodia.farmer.dto.farmer.FarmerResponse;
import org.springframework.stereotype.Component;

@Component
public class FarmerMapper {

    public Farmer toEntity(FarmerRequest request) {
        if (request == null) return null;

        Farmer farmer = new Farmer();
        farmer.setFullName(request.fullName());
        farmer.setPhone(request.phone());
        farmer.setProvince(request.province());
        farmer.setProfileImage(request.profileImage());
        return farmer;
    }

    public FarmerResponse toResponse(Farmer farmer) {
        if (farmer == null) return null;

        return new FarmerResponse(
                farmer.getId(),
                farmer.getFullName(),
                farmer.getPhone(),
                farmer.getProvince(),
                farmer.getProfileImage()
        );
    }
}