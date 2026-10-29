package co.cambodia.farmer.dto.message;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderName,
        UUID receiverId,
        String receiverName,
        String content,
        LocalDateTime timestamp
) {}
