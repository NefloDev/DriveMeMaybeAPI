package neflo.dev.group.api;

import java.util.Optional;

public record GroupRequestDTO(
        String name,
        Optional<String> pfp
) {
}
