package neflo.dev.user.api;

import java.util.Optional;

public record UserDTO(
        String email,
        Optional<String> password,
        String name,
        String nickname,
        Optional<String> pfp,
        UserPreferences preferences
) {
}
