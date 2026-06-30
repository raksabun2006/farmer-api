package co.cambodia.farmer.controller;

import co.cambodia.farmer.dto.message.MessageRequest;
import co.cambodia.farmer.dto.message.MessageResponse;
import co.cambodia.farmer.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        MessageResponse response = messageService.sendMessage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/conversation")
    public ResponseEntity<List<MessageResponse>> getConversation(
            @RequestParam("user1") Long user1,
            @RequestParam("user2") Long user2) {
        List<MessageResponse> conversation = messageService.getConversation(user1, user2);
        return ResponseEntity.ok(conversation);
    }
}