package co.cambodia.farmer.dto.farmer;

import java.util.UUID;

public record FarmerResponse(
        UUID id,
        String fullName,
        String phone,
        String province,
        String profileImage
) {}
