package neflo.dev.group.application;

import neflo.dev.group.api.*;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.mapper.GroupMapper;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.shared.exception.ValidationException;
import neflo.dev.shared.persistence.JDBCRepository;
import neflo.dev.trip.api.TripRequestDTO;
import neflo.dev.trip.domain.TripModel;
import neflo.dev.trip.infrastructure.mapper.TripMapper;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.mapper.UserMapper;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GroupServiceQueryTest {

    @Mock
    GroupRepository repository;

    @Mock
    UserRepository userRepository;

    @Mock
    JDBCRepository jdbcRepository;

    @Mock
    GroupMapper mapper;

    @Mock
    UserMapper userMapper;

    @Mock
    TripMapper tripMapper;

    @Mock
    GroupCodeGenerator codeGenerator;

    @InjectMocks
    GroupService underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static UserModel member(UUID id) {
        UserModel user = UserModel.builder().id(id).nickname("nick-" + id.toString().substring(0, 4)).build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    private static GroupModel groupWith(UserModel... members) {
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(members)));
        group.setTrips(new HashSet<>());
        return group;
    }

    @Test
    void should_returnBalances_when_member() {
        GroupModel group = groupWith(member(USER_ID));
        List<GroupMemberBalanceDTO> expected = List.of(new GroupMemberBalanceDTO("ana", 60, 12));

        given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
        given(jdbcRepository.getGroupMembersBalanceInfo(GROUP_ID)).willReturn(expected);

        assertThat(underTest.getGroupMembersBalance(USER_ID, GROUP_ID)).isEqualTo(expected);
    }

    @Test
    void should_throwValidation_when_balanceRequestedByNonMember() {
        given(repository.findById(GROUP_ID)).willReturn(Optional.of(groupWith(member(OTHER_ID))));

        assertThatThrownBy(() -> underTest.getGroupMembersBalance(USER_ID, GROUP_ID))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_returnMembers_when_member() {
        UserModel user = member(USER_ID);
        GroupModel group = groupWith(user);
        GroupMemberDTO dto = new GroupMemberDTO(USER_ID, "ana");

        given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
        given(userMapper.entityToGroupMemberDTO(user)).willReturn(dto);

        assertThat(underTest.getGroupMembers(USER_ID, GROUP_ID)).containsExactly(dto);
    }

    @Test
    void should_returnTrips_when_member() {
        UserModel user = member(USER_ID);
        GroupModel group = groupWith(user);
        TripModel trip = TripModel.builder().id(UUID.randomUUID()).date(LocalDate.of(2026, 5, 4)).build();
        trip.setPassengers(new HashSet<>());
        group.setTrips(new HashSet<>(Set.of(trip)));
        TripRequestDTO dto = new TripRequestDTO("ana", LocalDate.of(2026, 5, 4), 30, "A", "B", null, trip.getId());

        given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
        given(tripMapper.entityToRequestDTO(trip)).willReturn(dto);

        assertThat(underTest.getGroupTrips(USER_ID, GROUP_ID)).containsExactly(dto);
    }

    @Test
    void should_returnYearlyInsights_when_yearlyRequested() {
        GroupModel group = groupWith(member(USER_ID));
        GroupInsights expected = new GroupInsights(2026, null, Map.of("ana", 120));
        GroupInsightsRequest request = new GroupInsightsRequest(GroupInsightsPeriodTypes.YEARLY, 2026, null);

        given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
        given(jdbcRepository.getYearlyGroupInsights(GROUP_ID, 2026)).willReturn(expected);

        assertThat(underTest.getGroupInsights(USER_ID, GROUP_ID, request)).isEqualTo(expected);
    }

    @Test
    void should_returnMonthlyInsights_when_monthlyRequested() {
        GroupModel group = groupWith(member(USER_ID));
        GroupInsights expected = new GroupInsights(2026, 5, Map.of("ana", 45));
        GroupInsightsRequest request = new GroupInsightsRequest(GroupInsightsPeriodTypes.MONTHLY, 2026, 5);

        given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
        given(jdbcRepository.getMonthlyGroupInsights(eq(GROUP_ID), any())).willReturn(expected);

        assertThat(underTest.getGroupInsights(USER_ID, GROUP_ID, request)).isEqualTo(expected);
    }

    @Test
    void should_throwNotFound_when_queryingMissingGroup() {
        given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getGroupMembers(USER_ID, GROUP_ID))
                .isInstanceOf(NoEntitiesFoundException.class);
    }

    @Test
    void should_throwNotFound_when_balanceQueryingMissingGroup() {
        given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getGroupMembersBalance(USER_ID, GROUP_ID))
                .isInstanceOf(NoEntitiesFoundException.class);
    }

    @Test
    void should_throwNotFound_when_tripsQueryingMissingGroup() {
        given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getGroupTrips(USER_ID, GROUP_ID))
                .isInstanceOf(NoEntitiesFoundException.class);
    }

    @Test
    void should_throwNotFound_when_insightsQueryingMissingGroup() {
        var request = new GroupInsightsRequest(GroupInsightsPeriodTypes.YEARLY, 2026, null);

        given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getGroupInsights(USER_ID, GROUP_ID, request))
                .isInstanceOf(NoEntitiesFoundException.class);
    }
}
