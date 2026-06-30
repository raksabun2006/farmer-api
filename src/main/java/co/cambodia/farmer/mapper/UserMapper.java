package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.User;
import co.cambodia.farmer.dto.auth.RegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());

        return user;
    }
}