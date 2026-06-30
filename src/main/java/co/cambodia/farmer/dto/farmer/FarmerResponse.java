package co.cambodia.farmer.dto.farmer;

public record FarmerResponse(
        Long id,
        String fullName,
        String phone,
        String province,
        String profileImage
) {}