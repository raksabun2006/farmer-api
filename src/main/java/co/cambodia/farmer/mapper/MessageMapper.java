package co.cambodia.farmer.mapper;



import co.cambodia.farmer.domain.Message;
import co.cambodia.farmer.dto.message.MessageResponse;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }

        String senderName = message.getSender() != null ? message.getSender().getUsername() : "Unknown";
        String receiverName = message.getReceiver() != null ? message.getReceiver().getUsername() : "Unknown";

        return new MessageResponse(
                message.getId(),
                message.getSender() != null ? message.getSender().getId() : null,
                senderName,
                message.getReceiver() != null ? message.getReceiver().getId() : null,
                receiverName,
                message.getContent(),
                message.getTimestamp()
        );
    }
}