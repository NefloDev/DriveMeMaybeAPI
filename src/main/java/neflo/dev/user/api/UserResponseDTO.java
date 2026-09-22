package neflo.dev.user.api;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        String name,
        String nickname,
        String pfp,
        UserPreferences preferences
) {
}
