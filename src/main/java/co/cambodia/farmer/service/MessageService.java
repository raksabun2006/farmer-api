package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.message.MessageRequest;
import co.cambodia.farmer.dto.message.MessageResponse;

import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(MessageRequest request);
    List<MessageResponse> getConversation(Long user1, Long user2);
}