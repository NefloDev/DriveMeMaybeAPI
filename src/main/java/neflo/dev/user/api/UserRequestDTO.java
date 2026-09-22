package neflo.dev.user.api;

import java.util.UUID;

public record UserRequestDTO(
        String email,
        UUID uuid
) {
}
