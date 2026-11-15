package neflo.dev.trip.application;

import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.shared.exception.ValidationException;
import neflo.dev.shared.persistence.JDBCRepository;
import neflo.dev.trip.api.TripCreateDTO;
import neflo.dev.trip.api.TripDTO;
import neflo.dev.trip.domain.TripModel;
import neflo.dev.trip.infrastructure.mapper.TripMapper;
import neflo.dev.trip.infrastructure.persistence.TripRepository;
import neflo.dev.user.domain.UserModel;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TripServiceModificationTest {

    @Mock
    TripRepository repository;

    @Mock
    UserRepository userRepository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    JDBCRepository jdbcRepository;

    @Mock
    TripMapper mapper;

    @InjectMocks
    TripService underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DRIVER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PASSENGER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GROUP_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TRIP_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static UserModel user(UUID id) {
        UserModel u = UserModel.builder().id(id).email(id + "@example.com").nickname("nick-" + id.toString().substring(0, 4)).build();
        u.setGroups(new HashSet<>());
        u.setTrips(new HashSet<>());
        return u;
    }

    private TripModel persistedTrip(UserModel driver, UserModel passenger, GroupModel group) {
        TripModel trip = TripModel.builder()
                .id(TRIP_ID).group(group).driver(driver)
                .date(LocalDate.of(2026, 6, 1)).durationMinutes(30).distanceKm(20)
                .origin("Timisoara").destination("Arad").notes("old")
                .build();
        trip.setPassengers(new HashSet<>(Set.of(passenger)));
        return trip;
    }

    private GroupModel groupWith(UserModel... members) {
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(members)));
        group.setTrips(new HashSet<>());
        for (UserModel m : members) {
            m.setGroups(new HashSet<>(Set.of(group)));
        }
        return group;
    }

    @Test
    void should_returnDetail_when_valid() {
        UserModel member = user(USER_ID);
        GroupModel group = groupWith(member);
        TripModel trip = persistedTrip(member, user(PASSENGER_ID), group);
        TripDTO expected = new TripDTO(TRIP_ID, member.getId(), "nick", trip.getDate(), 30, 20, "Timisoara", "Arad", "old");

        given(repository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(mapper.entityToDTO(trip)).willReturn(expected);

        assertThat(underTest.getTripDetail(USER_ID, GROUP_ID, TRIP_ID)).isEqualTo(expected);
    }

    @Test
    void should_throwValidation_when_tripFromOtherGroup() {
        UserModel member = user(USER_ID);
        GroupModel group = groupWith(member);
        GroupModel other = GroupModel.builder().id(UUID.randomUUID()).name("Other").groupCode("ZZZZ-0000").build();
        other.setMembers(new HashSet<>());
        other.setTrips(new HashSet<>());
        TripModel trip = persistedTrip(member, user(PASSENGER_ID), other);

        given(repository.findById(TRIP_ID)).willReturn(Optional.of(trip));

        assertThatThrownBy(() -> underTest.getTripDetail(USER_ID, GROUP_ID, TRIP_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Non related groups");
    }

    @Test
    void should_throwValidation_when_viewerNotMember() {
        GroupModel group = groupWith(user(DRIVER_ID));
        TripModel trip = persistedTrip(user(DRIVER_ID), user(PASSENGER_ID), group);

        given(repository.findById(TRIP_ID)).willReturn(Optional.of(trip));

        assertThatThrownBy(() -> underTest.getTripDetail(USER_ID, GROUP_ID, TRIP_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Non members");
    }

    @Test
    void should_throwNotFound_when_tripMissing() {
        given(repository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getTripDetail(USER_ID, GROUP_ID, TRIP_ID))
                .isInstanceOf(NoEntitiesFoundException.class);
    }

    @Test
    void should_throwNotFound_when_updatingMissingTrip() {
        var dto = new TripCreateDTO(DRIVER_ID, LocalDate.of(2026, 6, 1), 30, 20, "A", "B", null, List.of(PASSENGER_ID));

        given(repository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.updateTrip(USER_ID, GROUP_ID, TRIP_ID, dto))
                .isInstanceOf(NoEntitiesFoundException.class);

        then(repository).should(never()).save(any());
    }

    @Test
    void should_deleteTrip_when_valid() {
        UserModel member = user(USER_ID);
        GroupModel group = groupWith(member);
        TripModel trip = persistedTrip(member, user(PASSENGER_ID), group);

        given(repository.findById(TRIP_ID)).willReturn(Optional.of(trip));

        underTest.deleteTrip(USER_ID, GROUP_ID, TRIP_ID);

        then(repository).should().delete(trip);
    }

    @Test
    void should_throwNotFound_when_deletingMissingTrip() {
        given(repository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.deleteTrip(USER_ID, GROUP_ID, TRIP_ID))
                .isInstanceOf(NoEntitiesFoundException.class);

        then(repository).should(never()).delete(any());
    }

    @Test
    void should_updateTrip_when_fieldsChanged() {
        UserModel member = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        UserModel newPassenger = user(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        GroupModel group = groupWith(member, driver, passenger, newPassenger);
        TripModel trip = persistedTrip(driver, passenger, group);
        TripCreateDTO dto = new TripCreateDTO(DRIVER_ID, LocalDate.of(2026, 6, 1), 30, 20,
                "NewOrigin", "NewDest", "NewNotes", List.of(newPassenger.getId()));
        TripDTO expected = new TripDTO(TRIP_ID, DRIVER_ID, "nick", dto.date(), 30, 20, "NewOrigin", "NewDest", "NewNotes");

        given(repository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(userRepository.findById(newPassenger.getId())).willReturn(Optional.of(newPassenger));
        given(userRepository.findById(DRIVER_ID)).willReturn(Optional.of(driver));
        given(repository.save(trip)).willReturn(trip);
        given(mapper.entityToDTO(trip)).willReturn(expected);

        TripDTO result = underTest.updateTrip(USER_ID, GROUP_ID, TRIP_ID, dto);

        assertThat(result).isEqualTo(expected);
        assertThat(trip.getOrigin()).isEqualTo("NewOrigin");
        assertThat(trip.getDestination()).isEqualTo("NewDest");
        assertThat(trip.getNotes()).isEqualTo("NewNotes");
        then(jdbcRepository).should(times(4)).updateGroupMemberBalance(any(), any(), any(int.class), any(int.class));
    }

    @Test
    void should_throwDatabase_when_nothingUpdated() {
        UserModel member = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        GroupModel group = groupWith(member, driver, passenger);
        TripModel trip = persistedTrip(driver, passenger, group);
        TripCreateDTO dto = new TripCreateDTO(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                LocalDate.of(2027, 1, 1), 999, 999,
                "Timisoara", "Arad", "old",
                List.of(passenger.getId()));

        given(repository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(userRepository.findById(passenger.getId())).willReturn(Optional.of(passenger));
        given(userRepository.findById(dto.driver())).willReturn(Optional.empty());
        given(repository.save(trip)).willReturn(trip);

        assertThatThrownBy(() -> underTest.updateTrip(USER_ID, GROUP_ID, TRIP_ID, dto))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("couldn't be updated");
    }
}
