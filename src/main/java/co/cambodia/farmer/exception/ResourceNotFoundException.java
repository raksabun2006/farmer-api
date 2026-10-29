package co.cambodia.farmer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

// This annotation automatically instructs Spring Boot to return a 404 Status instead of a 500 Status
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // Constructor that accepts a custom message
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, UUID id) {
        super(id != null ? message + " " + id : message);
    }
}
