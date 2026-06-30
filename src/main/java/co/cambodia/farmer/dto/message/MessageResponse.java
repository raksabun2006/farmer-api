package co.cambodia.farmer.dto.message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String content,
        LocalDateTime timestamp
) {}