package co.cambodia.farmer.dto.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name
) {}
