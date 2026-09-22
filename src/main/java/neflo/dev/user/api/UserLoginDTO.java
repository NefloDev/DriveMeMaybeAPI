package neflo.dev.user.api;

public record UserLoginDTO(
        String email,
        String password
) {
}
