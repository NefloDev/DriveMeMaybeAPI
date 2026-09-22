package neflo.dev.trip.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class TripService {

    private static final String CLASS_PATH = "DriveMeMaybeAPI.TripService";

    private final TripRepository repository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final JDBCRepository jdbcRepository;
    private final TripMapper mapper;

    private static void checkValidRequest(UUID userUuid, UUID groupUuid, TripModel repositoryResponse) {
        if (!repositoryResponse.getGroup().getId().equals(groupUuid)) {
            throw new ValidationException("non-group-access", "Non related groups cannot access external trips");
        }

        if (repositoryResponse.getGroup().getMembers().stream().noneMatch(m -> m.getId().equals(userUuid))) {
            throw new ValidationException("non-member-access", "Non members cannot access a group's trip data");
        }
    }

    public TripDTO createTrip(UUID userUuid, UUID groupUuid, TripCreateDTO dto) {
        log.info("{}.createTrip() >> tripDto :: {}", CLASS_PATH, dto);
        UserModel foundUser = getUserOrThrow(userUuid);
        log.info("{}.createTrip() >> user found", CLASS_PATH);

        GroupModel foundGroup = groupRepository.findByIdAndMembers_IdIn(groupUuid, List.of(dto.driver()))
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found with selected member."));
        log.info("{}.createTrip() >> group found", CLASS_PATH);

        List<UserModel> passengers = getPassengersOrThrow(dto.passengers(), foundGroup);

        if (foundGroup.getMembers().stream().noneMatch(m -> m.getId().equals(foundUser.getId()))) {
            throw new ValidationException("non-member-access", "Non members cannot create a trip in an external group");
        }

        UserModel driver = foundGroup.getMembers().stream().filter(m -> m.getId().equals(dto.driver())).findFirst().orElse(null);
        if (foundGroup.getMembers().stream().noneMatch(m -> m.getId().equals(foundUser.getId()))) {
            throw new ValidationException("invalid-driver", "Selected driver is not valid");
        }
        if (passengers.stream().anyMatch(m -> m.getId().equals(driver.getId()))) {
            throw new ValidationException("driver-passenger", "Selected driver cannot be a passenger at the same time");
        }
        if (dto.distanceKm() == null || dto.distanceKm() == 0) {
            throw new ValidationException("invalid-distance", "Trip's distance must be provided");
        }

        int durationMinutes = Objects.requireNonNullElse(dto.durationMinutes(), 0);
        int distanceKm = dto.distanceKm();
        TripModel trip = TripModel.builder()
                .driver(driver)
                .group(foundGroup)
                .date(dto.date())
                .durationMinutes(durationMinutes)
                .distanceKm(distanceKm)
                .origin(dto.origin())
                .destination(dto.destination())
                .notes(dto.notes())
                .passengers(new HashSet<>(passengers))
                .build();

        TripModel repositoryResponse = repository.save(trip);

        updateTripMembersBalances(driver, foundGroup, durationMinutes, distanceKm, passengers);

        TripDTO response = mapper.entityToDTO(repositoryResponse);
        log.info("{}.createTrip() >> trip created :: {}", CLASS_PATH, response);
        return response;
    }

    private void undoTripMembersBalances(TripModel trip) {
        jdbcRepository.updateGroupMemberBalance(trip.getDriver().getId(), trip.getGroup().getId(), trip.getDurationMinutes() * -1, trip.getDistanceKm() * -1);

        for (UserModel passenger : trip.getPassengers()) {
            jdbcRepository.updateGroupMemberBalance(passenger.getId(), trip.getGroup().getId(), trip.getDurationMinutes(), trip.getDistanceKm());
        }
    }

    private void updateTripMembersBalances(UserModel driver, GroupModel foundGroup, int durationMinutes, int distanceKm, List<UserModel> passengers) {
        jdbcRepository.updateGroupMemberBalance(driver.getId(), foundGroup.getId(), durationMinutes, distanceKm);

        for (UserModel passenger : passengers) {
            jdbcRepository.updateGroupMemberBalance(passenger.getId(), foundGroup.getId(), durationMinutes * -1, distanceKm * -1);
        }
    }

    private @NonNull List<UserModel> getPassengersOrThrow(List<UUID> passengersUuids, GroupModel foundGroup) {
        if (passengersUuids.isEmpty()) {
            throw new ValidationException("passenger-not-informed", "At least one passenger is required.");
        }
        List<UserModel> passengers = passengersUuids.stream()
                .distinct()
                .map(this::getUserOrThrow)
                .toList();

        for (UserModel passenger : passengers) {
            if (passenger.getGroups().stream().noneMatch(g -> g.getId().equals(foundGroup.getId()))) {
                throw new ValidationException("passenger-not-member", "All of the passengers should be members of the trip's group.");
            }
        }

        return passengers;
    }

    private @NonNull UserModel getUserOrThrow(UUID userUuid) {
        UserModel foundUser = userRepository.findById(userUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("user-not-found", "User not found."));
        return foundUser;
    }

    public TripDTO updateTrip(UUID userUuid, UUID groupUuid, UUID tripUuid, TripCreateDTO dto) {
        log.info("{}.updateTrip() >> tripUuid :: {} -- dto :: {}", CLASS_PATH, tripUuid, dto);

        TripModel repositoryResponse = repository.findById(tripUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("trip-not-found", "Trip not found."));

        checkValidRequest(userUuid, groupUuid, repositoryResponse);
        undoTripMembersBalances(repositoryResponse);

        List<UserModel> passengers = getPassengersOrThrow(dto.passengers(), repositoryResponse.getGroup());

        mapper.updateEntity(repositoryResponse, dto);
        int updateCount = 0;

        if (dto.origin() != null && !dto.origin().equals(repositoryResponse.getOrigin())) {
            updateCount++;
            repositoryResponse.setOrigin(dto.origin());
        }

        if (dto.destination() != null && !dto.destination().equals(repositoryResponse.getDestination())) {
            updateCount++;
            repositoryResponse.setDestination(dto.destination());
        }

        if (dto.notes() != null && !dto.notes().equals(repositoryResponse.getNotes())) {
            updateCount++;
            repositoryResponse.setNotes(dto.notes());
        }

        if (!passengers.isEmpty() &&
                repositoryResponse.getPassengers().isEmpty() || !repositoryResponse.getPassengers().stream()
                    .map(p -> p.getId().toString()).toList().equals(passengers.stream().map(p -> p.getId().toString()).toList())) {
            updateCount++;
            repositoryResponse.setPassengers(new HashSet<>(passengers));
        }

        Optional<UserModel> driver = userRepository.findById(dto.driver());
        if (driver.isPresent()) {
            repositoryResponse.setDriver(driver.get());
        }

        repositoryResponse = repository.save(repositoryResponse);

        if (!repositoryResponse.equalsDto(dto) && updateCount == 0) {
            throw new DatabaseException("trip-not-updated", "Trip couldn't be updated at the moment, try again later.");
        }

        int durationMinutes = Objects.requireNonNullElse(repositoryResponse.getDurationMinutes(), 0);
        int distanceKm = repositoryResponse.getDistanceKm();
        updateTripMembersBalances(repositoryResponse.getDriver(), repositoryResponse.getGroup(), durationMinutes, distanceKm, passengers);

        log.info("{}.updateTrip() >> trip updated successfully", CLASS_PATH);
        return mapper.entityToDTO(repositoryResponse);
    }

    public void deleteTrip(UUID userUuid, UUID groupUuid, UUID tripUuid) {
        log.info("{}.deleteTrip() >> tripUuid :: {}", CLASS_PATH, tripUuid);

        TripModel repositoryResponse = repository.findById(tripUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("trip-not-found", "Trip not found."));

        checkValidRequest(userUuid, groupUuid, repositoryResponse);

        repository.delete(repositoryResponse);
    }

    public TripDTO getTripDetail(UUID userUuid, UUID groupUuid, UUID tripUuid) {
        log.info("{}.getTripDetail() >> tripUuid :: {}", CLASS_PATH, tripUuid);

        TripModel repositoryResponse = repository.findById(tripUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("trip-not-found", "Trip not found."));

        checkValidRequest(userUuid, groupUuid, repositoryResponse);

        log.info("{}.getTripDetail() >> trip found :: {}", CLASS_PATH, repositoryResponse);
        return mapper.entityToDTO(repositoryResponse);
    }


}
