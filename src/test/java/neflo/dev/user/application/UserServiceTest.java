package neflo.dev.user.application;

import neflo.dev.group.api.GroupDTO;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.mapper.GroupMapper;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.user.api.UserDTO;
import neflo.dev.user.api.UserPreferences;
import neflo.dev.user.api.UserPreferencesFilters;
import neflo.dev.user.api.UserResponseDTO;
import neflo.dev.user.domain.LanguagesModel;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.mapper.UserMapper;
import neflo.dev.user.infrastructure.persistence.LanguagesRepository;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    LanguagesRepository langRepository;

    @Mock
    UserMapper mapper;

    @Mock
    GroupMapper groupMapper;

    @InjectMocks
    UserService underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static UserModel persistedUser() {
        UserModel user = UserModel.builder()
                .id(USER_ID)
                .email("ana@example.com")
                .password("encoded-old")
                .name("Ana")
                .nickname("ana")
                .pfp("old-pfp")
                .build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    private static UserDTO updateDto(Optional<String> password, Optional<String> pfp) {
        return new UserDTO(
                "ana@example.com",
                password,
                "Ana",
                "ana",
                pfp,
                new UserPreferences(true, "EN"));
    }

    private static UserResponseDTO responseDto() {
        return new UserResponseDTO(USER_ID, "ana@example.com", "Ana", "ana", "old-pfp",
                new UserPreferences(true, "English"));
    }

    @Nested
    class Retrieval {

        @Test
        void should_returnDetail_when_userExists() {
            UserModel user = persistedUser();
            UserResponseDTO expected = responseDto();

            given(repository.findById(USER_ID)).willReturn(Optional.of(user));
            given(mapper.entityToResponseDTO(user)).willReturn(expected);

            UserResponseDTO result = underTest.getUserDetail(USER_ID);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void should_throwNotFound_when_userMissing() {
            given(repository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.getUserDetail(USER_ID))
                    .isInstanceOf(NoEntitiesFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void should_returnSortedLanguages_when_filtersRequested() {
            LanguagesModel spanish = new LanguagesModel(UUID.randomUUID(), "ES", "Spanish");
            LanguagesModel english = new LanguagesModel(UUID.randomUUID(), "EN", "English");

            given(langRepository.findAll()).willReturn(List.of(spanish, english));
            given(mapper.languageEntityToDTO(spanish))
                    .willReturn(new UserPreferencesFilters.LanguageDTO("ES", "Spanish"));
            given(mapper.languageEntityToDTO(english))
                    .willReturn(new UserPreferencesFilters.LanguageDTO("EN", "English"));

            UserPreferencesFilters result = underTest.getUserPreferencesFilters();

            assertThat(result.languageSelection())
                    .extracting(UserPreferencesFilters.LanguageDTO::code)
                    .containsExactly("EN", "ES");
        }

        @Test
        void should_returnGroups_when_userExists() {
            UserModel user = persistedUser();
            GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
            group.setMembers(new HashSet<>());
            group.setTrips(new HashSet<>());
            user.setGroups(new HashSet<>(Set.of(group)));
            GroupDTO groupDto = new GroupDTO(GROUP_ID, "Riders", "ABCD-1234", 1, 0, Optional.empty());

            given(repository.findById(USER_ID)).willReturn(Optional.of(user));
            given(groupMapper.entityToDTO(group)).willReturn(groupDto);

            List<GroupDTO> result = underTest.getUserGroups(USER_ID);

            assertThat(result).containsExactly(groupDto);
        }

        @Test
        void should_throwNotFound_when_groupsRequestedForMissingUser() {
            given(repository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.getUserGroups(USER_ID))
                    .isInstanceOf(NoEntitiesFoundException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void should_updatePassword_when_passwordChanged() {
            UserModel user = persistedUser();
            UserDTO dto = updateDto(Optional.of("new-secret"), Optional.empty());
            LanguagesModel lang = new LanguagesModel(UUID.randomUUID(), "EN", "English");
            UserResponseDTO expected = responseDto();

            given(repository.findById(USER_ID)).willReturn(Optional.of(user));
            given(langRepository.findByCode("EN")).willReturn(lang);
            given(repository.save(user)).willReturn(user);
            given(mapper.entityToResponseDTO(user)).willReturn(expected);

            UserResponseDTO result = underTest.updateUser(USER_ID, dto);

            assertThat(result).isEqualTo(expected);
            assertThat(user.getPassword()).isEqualTo("new-secret");
        }

        @Test
        void should_updatePfp_when_pfpChanged() {
            UserModel user = persistedUser();
            UserDTO dto = updateDto(Optional.empty(), Optional.of("new-pfp"));
            LanguagesModel lang = new LanguagesModel(UUID.randomUUID(), "EN", "English");
            UserResponseDTO expected = responseDto();

            given(repository.findById(USER_ID)).willReturn(Optional.of(user));
            given(langRepository.findByCode("EN")).willReturn(lang);
            given(repository.save(user)).willReturn(user);
            given(mapper.entityToResponseDTO(user)).willReturn(expected);

            underTest.updateUser(USER_ID, dto);

            assertThat(user.getPfp()).isEqualTo("new-pfp");
        }

        @Test
        void should_throwDatabase_when_nothingChangedAndDtoNotEqual() {
            UserModel user = persistedUser();
            user.setPassword("same");
            user.setPfp("same-pfp");
            UserDTO dto = new UserDTO("other@example.com", Optional.of("same"), "Other", "other",
                    Optional.of("same-pfp"), new UserPreferences(false, "EN"));
            LanguagesModel lang = new LanguagesModel(UUID.randomUUID(), "EN", "English");

            given(repository.findById(USER_ID)).willReturn(Optional.of(user));
            given(langRepository.findByCode("EN")).willReturn(lang);
            given(repository.save(user)).willReturn(user);

            assertThatThrownBy(() -> underTest.updateUser(USER_ID, dto))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("User couldn't be updated");
        }

        @Test
        void should_throwNotFound_when_updatingMissingUser() {
            given(repository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.updateUser(USER_ID, updateDto(Optional.empty(), Optional.empty())))
                    .isInstanceOf(NoEntitiesFoundException.class);

            then(repository).should(never()).save(any());
        }
    }

    @Nested
    class Deletion {

        @Test
        void should_delete_when_userDisappears() {
            given(repository.findById(USER_ID)).willReturn(Optional.empty());

            underTest.deleteUser(USER_ID);

            then(repository).should().deleteById(USER_ID);
        }

        @Test
        void should_throwDatabase_when_userStillPresent() {
            given(repository.findById(USER_ID)).willReturn(Optional.of(persistedUser()));

            assertThatThrownBy(() -> underTest.deleteUser(USER_ID))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("User couldn't be deleted");
        }
    }
}
