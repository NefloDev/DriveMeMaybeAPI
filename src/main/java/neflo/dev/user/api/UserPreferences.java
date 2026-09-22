package neflo.dev.user.api;

public record UserPreferences(
        Boolean isDarkMode,
        String preferredLang
) {
}
