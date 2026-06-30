package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.Message;
import co.cambodia.farmer.domain.User;
import co.cambodia.farmer.dto.message.MessageRequest;
import co.cambodia.farmer.dto.message.MessageResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.MessageMapper;
import co.cambodia.farmer.repository.MessageRepository;
import co.cambodia.farmer.repository.UserRepository;
import co.cambodia.farmer.service.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public MessageResponse sendMessage(MessageRequest request) {
        User sender = userRepository.findById(request.senderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with ID: " + request.senderId()));

        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with ID: " + request.receiverId()));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.content());

        Message savedMessage = messageRepository.save(message);
        return messageMapper.toResponse(savedMessage);
    }

    @Override
    @Transactional
    public List<MessageResponse> getConversation(Long user1, Long user2) {
        return messageRepository.findConversation(user1, user2)
                .stream()
                .map(messageMapper::toResponse)
                .collect(Collectors.toList());
    }
}