package neflo.dev.user.infrastructure.mapper;

import neflo.dev.group.api.GroupMemberDTO;
import neflo.dev.user.api.UserDTO;
import neflo.dev.user.api.UserPreferences;
import neflo.dev.user.api.UserPreferencesFilters;
import neflo.dev.user.api.UserResponseDTO;
import neflo.dev.user.domain.LanguagesModel;
import neflo.dev.user.domain.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "pfp", ignore = true)
    @Mapping(target = "prefLang", ignore = true)
    @Mapping(target = "prefDarkMode", source = "preferences.isDarkMode")
    void updateEntity(@MappingTarget UserModel userModel, UserDTO dto);

    @Mapping(target = "preferences", source = "entity")
    UserResponseDTO entityToResponseDTO(UserModel entity);

    @Mapping(target = "isDarkMode", source = "prefDarkMode")
    @Mapping(target = "preferredLang", source = "prefLang.formalName")
    UserPreferences entityToUserPrefs(UserModel entity);

    UserPreferencesFilters.LanguageDTO languageEntityToDTO(LanguagesModel entity);

    GroupMemberDTO entityToGroupMemberDTO(UserModel entity);

}
