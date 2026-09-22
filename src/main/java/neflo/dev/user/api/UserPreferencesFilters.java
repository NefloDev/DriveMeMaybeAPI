package neflo.dev.user.api;

import java.util.List;

public record UserPreferencesFilters(
        List<LanguageDTO> languageSelection
) {

    public record LanguageDTO(
            String code,
            String formalName
    ){}

}
