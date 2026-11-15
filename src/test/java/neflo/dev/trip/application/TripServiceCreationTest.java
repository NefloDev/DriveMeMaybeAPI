package neflo.dev.trip.application;

import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
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

// Note: 5 collaborators exceed max-3-mocks; TripService mixes orchestration + balance updates.
@ExtendWith(MockitoExtension.class)
class TripServiceCreationTest {

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

    private static UserModel user(UUID id) {
        UserModel u = UserModel.builder().id(id).email(id + "@example.com").name("Name").nickname("nick").build();
        u.setGroups(new HashSet<>());
        u.setTrips(new HashSet<>());
        return u;
    }

    private TripCreateDTO dto(UUID driver, List<UUID> passengers, Integer distance, Integer duration) {
        return new TripCreateDTO(driver, LocalDate.of(2026, 6, 1), duration, distance, "Timisoara", "Arad", "notes", passengers);
    }

    private void givenGroupWith(UserModel... members) {
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(members)));
        group.setTrips(new HashSet<>());
        for (UserModel m : members) {
            m.setGroups(new HashSet<>(Set.of(group)));
        }

        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID)))
                .willReturn(Optional.of(group));
    }

    @Test
    void should_createTrip_when_valid() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver, passenger)));
        group.setTrips(new HashSet<>());
        creator.setGroups(new HashSet<>(Set.of(group)));
        driver.setGroups(new HashSet<>(Set.of(group)));
        passenger.setGroups(new HashSet<>(Set.of(group)));

        TripCreateDTO input = dto(DRIVER_ID, List.of(PASSENGER_ID), 42, 60);
        TripModel saved = TripModel.builder().id(UUID.randomUUID()).group(group).driver(driver)
                .date(input.date()).durationMinutes(60).distanceKm(42).build();
        saved.setPassengers(new HashSet<>(Set.of(passenger)));
        TripDTO expected = new TripDTO(saved.getId(), DRIVER_ID, "nick", input.date(), 60, 42, "Timisoara", "Arad", "notes");

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(PASSENGER_ID)).willReturn(Optional.of(passenger));
        given(repository.save(any(TripModel.class))).willReturn(saved);
        given(mapper.entityToDTO(saved)).willReturn(expected);

        TripDTO result = underTest.createTrip(USER_ID, GROUP_ID, input);

        assertThat(result).isEqualTo(expected);
        then(jdbcRepository).should().updateGroupMemberBalance(DRIVER_ID, GROUP_ID, 60, 42);
        then(jdbcRepository).should().updateGroupMemberBalance(PASSENGER_ID, GROUP_ID, -60, -42);
    }

    @Test
    void should_defaultDuration_when_null() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver, passenger)));
        group.setTrips(new HashSet<>());
        creator.setGroups(new HashSet<>(Set.of(group)));
        driver.setGroups(new HashSet<>(Set.of(group)));
        passenger.setGroups(new HashSet<>(Set.of(group)));

        TripCreateDTO input = dto(DRIVER_ID, List.of(PASSENGER_ID), 10, null);
        TripModel saved = TripModel.builder().id(UUID.randomUUID()).group(group).driver(driver)
                .date(input.date()).durationMinutes(0).distanceKm(10).build();
        saved.setPassengers(new HashSet<>(Set.of(passenger)));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(PASSENGER_ID)).willReturn(Optional.of(passenger));
        given(repository.save(any(TripModel.class))).willReturn(saved);
        given(mapper.entityToDTO(saved)).willReturn(new TripDTO(saved.getId(), DRIVER_ID, "nick", input.date(), 0, 10, "T", "A", null));

        underTest.createTrip(USER_ID, GROUP_ID, input);

        then(jdbcRepository).should().updateGroupMemberBalance(DRIVER_ID, GROUP_ID, 0, 10);
    }

    @Test
    void should_throwNotFound_when_userMissing() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(PASSENGER_ID), 10, 5)))
                .isInstanceOf(NoEntitiesFoundException.class);
    }

    @Test
    void should_throwNotFound_when_groupMissing() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(PASSENGER_ID), 10, 5)))
                .isInstanceOf(NoEntitiesFoundException.class)
                .hasMessageContaining("Group not found");

        then(repository).should(never()).save(any());
    }

    @Test
    void should_throwValidation_when_noPassengers() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver)));
        group.setTrips(new HashSet<>());

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(), 10, 5)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("At least one passenger");
    }

    @Test
    void should_throwValidation_when_passengerNotMember() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel outsider = user(PASSENGER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver)));
        group.setTrips(new HashSet<>());
        creator.setGroups(new HashSet<>(Set.of(group)));
        driver.setGroups(new HashSet<>(Set.of(group)));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(PASSENGER_ID)).willReturn(Optional.of(outsider));

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(PASSENGER_ID), 10, 5)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("passengers should be members");
    }

    @Test
    void should_throwValidation_when_creatorNotMember() {
        UserModel outsider = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(driver, passenger)));
        group.setTrips(new HashSet<>());
        driver.setGroups(new HashSet<>(Set.of(group)));
        passenger.setGroups(new HashSet<>(Set.of(group)));
        outsider.setGroups(new HashSet<>());

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(outsider));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(PASSENGER_ID)).willReturn(Optional.of(passenger));

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(PASSENGER_ID), 10, 5)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Non members cannot create");
    }

    @Test
    void should_throwValidation_when_driverIsPassenger() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver)));
        group.setTrips(new HashSet<>());
        creator.setGroups(new HashSet<>(Set.of(group)));
        driver.setGroups(new HashSet<>(Set.of(group)));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(DRIVER_ID)).willReturn(Optional.of(driver));

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(DRIVER_ID), 10, 5)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be a passenger");
    }

    @Test
    void should_throwValidation_when_distanceNull() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver, passenger)));
        group.setTrips(new HashSet<>());
        creator.setGroups(new HashSet<>(Set.of(group)));
        driver.setGroups(new HashSet<>(Set.of(group)));
        passenger.setGroups(new HashSet<>(Set.of(group)));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(PASSENGER_ID)).willReturn(Optional.of(passenger));

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(PASSENGER_ID), null, 5)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("distance must be provided");
    }

    @Test
    void should_throwValidation_when_distanceZero() {
        UserModel creator = user(USER_ID);
        UserModel driver = user(DRIVER_ID);
        UserModel passenger = user(PASSENGER_ID);
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>(Set.of(creator, driver, passenger)));
        group.setTrips(new HashSet<>());
        creator.setGroups(new HashSet<>(Set.of(group)));
        driver.setGroups(new HashSet<>(Set.of(group)));
        passenger.setGroups(new HashSet<>(Set.of(group)));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
        given(groupRepository.findByIdAndMembers_IdIn(GROUP_ID, List.of(DRIVER_ID))).willReturn(Optional.of(group));
        given(userRepository.findById(PASSENGER_ID)).willReturn(Optional.of(passenger));

        assertThatThrownBy(() -> underTest.createTrip(USER_ID, GROUP_ID, dto(DRIVER_ID, List.of(PASSENGER_ID), 0, 5)))
                .isInstanceOf(ValidationException.class);
    }
}
