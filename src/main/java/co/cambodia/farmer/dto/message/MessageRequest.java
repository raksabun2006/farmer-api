package co.cambodia.farmer.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MessageRequest(
        @NotNull(message = "Sender ID cannot be null")
        UUID senderId,

        @NotNull(message = "Receiver ID cannot be null")
        UUID receiverId,

        @NotBlank(message = "Message content cannot be blank")
        @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
        String content
) {}
