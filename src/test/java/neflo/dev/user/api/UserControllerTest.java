package neflo.dev.user.api;

import neflo.dev.group.api.GroupDTO;
import neflo.dev.user.application.UserService;
import neflo.dev.user.domain.UserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService service;

    @InjectMocks
    UserController underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static UserModel principal() {
        UserModel user = UserModel.builder().id(USER_ID).email("ana@example.com").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    @Test
    void should_returnProfile_when_requested() {
        UserResponseDTO expected = new UserResponseDTO(USER_ID, "ana@example.com", "Ana", "ana", null,
                new UserPreferences(false, "EN"));

        given(service.getUserDetail(USER_ID)).willReturn(expected);

        var result = underTest.getUserProfile(principal());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnFilters_when_requested() {
        UserPreferencesFilters expected = new UserPreferencesFilters(List.of());

        given(service.getUserPreferencesFilters()).willReturn(expected);

        assertThat(underTest.getUserPreferencesFilters().getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnGroups_when_requested() {
        List<GroupDTO> expected = List.of(new GroupDTO(UUID.randomUUID(), "Riders", "ABCD-1234", 1, 0, Optional.empty()));

        given(service.getUserGroups(USER_ID)).willReturn(expected);

        assertThat(underTest.getUserGroups(principal()).getBody()).isEqualTo(expected);
    }

    @Test
    void should_joinGroup_when_codeGiven() {
        GroupDTO expected = new GroupDTO(UUID.randomUUID(), "Riders", "ABCD-1234", 1, 0, Optional.empty());

        given(service.joinGroup(USER_ID, "ABCD-1234")).willReturn(expected);

        assertThat(underTest.joinGroup(principal(), "ABCD-1234").getBody()).isEqualTo(expected);
    }

    @Test
    void should_updateUser_when_dtoGiven() {
        UserDTO dto = new UserDTO("ana@example.com", Optional.empty(), "Ana", "ana", Optional.empty(),
                new UserPreferences(false, "EN"));
        UserResponseDTO expected = new UserResponseDTO(USER_ID, "ana@example.com", "Ana", "ana", null,
                new UserPreferences(false, "EN"));

        given(service.updateUser(USER_ID, dto)).willReturn(expected);

        assertThat(underTest.updateUser(principal(), dto).getBody()).isEqualTo(expected);
    }

    @Test
    void should_deleteUser_when_requested() {
        var result = underTest.deleteUser(principal());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(service).should().deleteUser(USER_ID);
    }
}
