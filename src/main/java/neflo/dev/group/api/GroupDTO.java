package neflo.dev.group.api;

import java.util.Optional;
import java.util.UUID;

public record GroupDTO(
        UUID id,
        String name,
        String groupCode,
        Integer groupMembers,
        Integer groupTrips,
        Optional<String> pfp
) {
}
