package neflo.dev.group.api;

import neflo.dev.group.application.GroupService;
import neflo.dev.trip.api.TripRequestDTO;
import neflo.dev.user.domain.UserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    GroupService service;

    @InjectMocks
    GroupController underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static UserModel principal() {
        UserModel user = UserModel.builder().id(USER_ID).email("ana@example.com").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    @Test
    void should_createGroup_when_requested() {
        GroupRequestDTO request = new GroupRequestDTO("Riders", Optional.empty());
        GroupDTO expected = new GroupDTO(GROUP_ID, "Riders", "ABCD-1234", 1, 0, Optional.empty());

        given(service.createGroup(USER_ID, request)).willReturn(expected);

        assertThat(underTest.createGroup(principal(), request).getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnDetail_when_requested() {
        GroupDTO expected = new GroupDTO(GROUP_ID, "Riders", "ABCD-1234", 1, 0, Optional.empty());

        given(service.getGroupDetail(USER_ID, GROUP_ID)).willReturn(expected);

        assertThat(underTest.getGroupDetail(principal(), GROUP_ID).getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnBalances_when_requested() {
        List<GroupMemberBalanceDTO> expected = List.of(new GroupMemberBalanceDTO("ana", 10, 5));

        given(service.getGroupMembersBalance(USER_ID, GROUP_ID)).willReturn(expected);

        assertThat(underTest.getGroupMemberBalanceList(principal(), GROUP_ID).getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnMembers_when_requested() {
        List<GroupMemberDTO> expected = List.of(new GroupMemberDTO(USER_ID, "ana"));

        given(service.getGroupMembers(USER_ID, GROUP_ID)).willReturn(expected);

        assertThat(underTest.getGroupMemberList(principal(), GROUP_ID).getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnTrips_when_requested() {
        List<TripRequestDTO> expected = List.of(
                new TripRequestDTO("ana", LocalDate.of(2026, 5, 4), 30, "A", "B", null, UUID.randomUUID()));

        given(service.getGroupTrips(USER_ID, GROUP_ID)).willReturn(expected);

        assertThat(underTest.getGroupTrips(principal(), GROUP_ID).getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnInsights_when_requested() {
        GroupInsightsRequest request = new GroupInsightsRequest(GroupInsightsPeriodTypes.YEARLY, 2026, null);
        GroupInsights expected = new GroupInsights(2026, null, java.util.Map.of("ana", 60));

        given(service.getGroupInsights(USER_ID, GROUP_ID, request)).willReturn(expected);

        assertThat(underTest.getGroupInsights(principal(), GROUP_ID, request).getBody()).isEqualTo(expected);
    }

    @Test
    void should_updateGroup_when_requested() {
        GroupDTO dto = new GroupDTO(GROUP_ID, "New name", "ABCD-1234", 1, 0, Optional.empty());

        given(service.updateGroup(USER_ID, GROUP_ID, dto)).willReturn(dto);

        assertThat(underTest.updateGroup(principal(), GROUP_ID, dto).getBody()).isEqualTo(dto);
    }

    @Test
    void should_leaveGroup_when_requested() {
        var result = underTest.leaveGroup(principal(), GROUP_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(service).should().leaveGroup(USER_ID, GROUP_ID);
    }
}
