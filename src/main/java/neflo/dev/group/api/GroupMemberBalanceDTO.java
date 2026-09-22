package neflo.dev.group.api;

public record GroupMemberBalanceDTO(
        String nickname,
        int timeBalance,
        int kmBalance
) {
}
