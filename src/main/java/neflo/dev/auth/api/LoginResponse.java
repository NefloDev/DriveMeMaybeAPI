package neflo.dev.auth.api;

public record LoginResponse(
        String token,
        long expiresOn
) {
}
