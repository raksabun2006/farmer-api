package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.message.MessageRequest;
import co.cambodia.farmer.dto.message.MessageResponse;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    MessageResponse sendMessage(MessageRequest request);
    List<MessageResponse> getConversation(UUID user1, UUID user2);
}
