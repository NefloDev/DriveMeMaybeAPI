package neflo.dev.group.api;

import java.util.UUID;

public record GroupMemberDTO(
        UUID id,
        String nickname
) {
}
