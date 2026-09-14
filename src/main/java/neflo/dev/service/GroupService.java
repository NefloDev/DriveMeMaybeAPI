package neflo.dev.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.exceptions.DatabaseException;
import neflo.dev.exceptions.NoEntitiesFoundException;
import neflo.dev.exceptions.ValidationException;
import neflo.dev.mapper.GroupMapper;
import neflo.dev.mapper.TripMapper;
import neflo.dev.mapper.UserMapper;
import neflo.dev.model.dto.YearMonth;
import neflo.dev.model.dto.group.*;
import neflo.dev.model.dto.trip.TripRequestDTO;
import neflo.dev.model.entity.GroupModel;
import neflo.dev.model.entity.TripModel;
import neflo.dev.model.entity.UserModel;
import neflo.dev.repository.GroupRepository;
import neflo.dev.repository.JDBCRepository;
import neflo.dev.repository.UserRepository;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class GroupService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;
    private static final String CLASS_PATH = "DriveMeMaybeAPI.GroupService";

    private final GroupRepository repository;
    private final UserRepository userRepository;
    private final JDBCRepository jdbcRepository;
    private final GroupMapper mapper;
    private final UserMapper userMapper;
    private final TripMapper tripMapper;
    private final GroupCodeGenerator codeGenerator;

    private static void checkUserIsMember(UUID userUuid, GroupModel repositoryResponse) {
        if (repositoryResponse.getMembers().stream().noneMatch(m -> m.getId().equals(userUuid))) {
            throw new ValidationException("non-member-access", "Non members cannot access a group's data");
        }
    }

    public GroupDTO getGroupDetail(UUID userUuid, UUID groupUuid) {
        log.info("{}.getGroupDetail() >> groupUuid :: {}", CLASS_PATH, groupUuid);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));

        checkUserIsMember(userUuid, repositoryResponse);

        log.info("{}.getGroupDetail() >> group found :: {}", CLASS_PATH, repositoryResponse);
        return mapper.entityToDTO(repositoryResponse);
    }

    public GroupDTO updateGroup(UUID userUuid, UUID groupUuid, GroupDTO dto) {
        log.info("{}.updateGroup() >> groupUuid :: {} -- dto :: {}", CLASS_PATH, groupUuid, dto);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));

        checkUserIsMember(userUuid, repositoryResponse);

        mapper.updateEntity(repositoryResponse, dto);

        boolean hasUpdatedPfp = false;
        if (dto.pfp().isPresent()) {
            String pfpB64 = dto.pfp().get();
            if (!pfpB64.equals(repositoryResponse.getPfp())) {
                hasUpdatedPfp = true;
                repositoryResponse.setPfp(pfpB64);
            }
        }

        repositoryResponse = repository.save(repositoryResponse);

        if (!repositoryResponse.equalsDto(dto) && !hasUpdatedPfp) {
            throw new DatabaseException("group-not-updated", "Group couldn't be updated at the moment, try again later.");
        }

        log.info("{}.updateGroup() >> group updated successfully", CLASS_PATH);
        return mapper.entityToDTO(repositoryResponse);
    }

    public void leaveGroup(UUID userUuid, UUID groupUuid) {
        log.info("{}.leaveGroup() >> groupUuid :: {} -- userUuid :: {}", CLASS_PATH, groupUuid, userUuid);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));

        if (repositoryResponse.getMembers().size() == 1 && repositoryResponse.getMembers().stream().allMatch(m -> m.getId().equals(userUuid))) {
            repositoryResponse.getMembers().forEach(
                    user -> user.getGroups().remove(repositoryResponse)
            );
            repository.delete(repositoryResponse);
            log.info("{}.leaveGroup() >> group was deleted since there are no users related to it :: {}", CLASS_PATH, groupUuid);
            return;
        }

        boolean wasRemoved = repositoryResponse.getMembers().removeIf(m -> m.getId().equals(userUuid));

        if (wasRemoved) repository.save(repositoryResponse);
    }

    public List<GroupMemberBalanceDTO> getGroupMembersBalance(UUID userUuid, UUID groupUuid) {
        log.info("{}.getGroupMembersBalance() >> groupUuid :: {}", CLASS_PATH, groupUuid);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));
        log.info("{}.getGroupMembersBalance() >> group found", CLASS_PATH);

        checkUserIsMember(userUuid, repositoryResponse);

        List<GroupMemberBalanceDTO> groupMembers = jdbcRepository.getGroupMembersBalanceInfo(repositoryResponse.getId());
        log.info("{}.getGroupMembersBalance() >> groupMembers :: {}", CLASS_PATH, groupMembers.stream().map(GroupMemberBalanceDTO::nickname).toList());

        return groupMembers;
    }

    public List<GroupMemberDTO> getGroupMembers(UUID userUuid, UUID groupUuid) {
        log.info("{}.getGroupMembers() >> groupUuid :: {}", CLASS_PATH, groupUuid);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));
        log.info("{}.getGroupMembers() >> group found", CLASS_PATH);

        checkUserIsMember(userUuid, repositoryResponse);

        List<GroupMemberDTO> groupMembers = repositoryResponse.getMembers().stream().map(userMapper::entityToGroupMemberDTO).toList();
        log.info("{}.getGroupMembers() >> groupMembers :: {}", CLASS_PATH, groupMembers.stream().map(GroupMemberDTO::nickname).toList());

        return groupMembers;
    }

    public List<TripRequestDTO> getGroupTrips(UUID userUuid, UUID groupUuid) {
        log.info("{}.getGroupTrips() >> groupUuid :: {}", CLASS_PATH, groupUuid);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));
        log.info("{}.getGroupTrips() >> group found", CLASS_PATH);

        checkUserIsMember(userUuid, repositoryResponse);

        List<TripRequestDTO> groupMembers = repositoryResponse.getTrips().stream().map(tripMapper::entityToRequestDTO).toList();
        log.info("{}.getGroupTrips() >> groupMembers :: {}", CLASS_PATH, groupMembers.stream().map(TripRequestDTO::date).toList());

        return groupMembers;
    }

    public GroupInsights getGroupInsights(UUID userUuid, UUID groupUuid, GroupInsightsRequest groupInsightsRequest) {
        log.info("{}.getGroupInsights() >> groupUuid :: {} -- groupInsightsRequest :: {}", CLASS_PATH, groupUuid, groupInsightsRequest);

        GroupModel repositoryResponse = repository.findById(groupUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));
        log.info("{}.getGroupInsights() >> group found", CLASS_PATH);

        checkUserIsMember(userUuid, repositoryResponse);

        GroupInsights response = switch (groupInsightsRequest.periodType()) {
            case YEARLY -> jdbcRepository.getYearlyGroupInsights(groupUuid, groupInsightsRequest.year());
            case MONTHLY ->
                    jdbcRepository.getMonthlyGroupInsights(groupUuid, YearMonth.fromGroupInsightsRequest(groupInsightsRequest));
        };
        log.info("{}.getGroupInsights() >> insights found", CLASS_PATH);

        return response;
    }

    @Transactional
    public GroupDTO createGroup(UUID userUuid, GroupRequestDTO groupDTO) {
        log.info("{}.createGroup() >> groupDTO :: {}", CLASS_PATH, groupDTO);
        UserModel foundUser = userRepository.findById(userUuid)
                .orElseThrow(() -> new NoEntitiesFoundException("user-not-found", "User not found."));
        log.info("{}.createGroup() >> user found", CLASS_PATH);

        for (int attempt = 1; attempt <= MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            GroupModel groupModel = GroupModel.builder()
                    .name(groupDTO.name())
                    .groupCode(codeGenerator.generate())
                    .build();

            if (groupDTO.pfp().isPresent()) {
                groupModel.setPfp(groupDTO.pfp().get());
            }

            try {
                groupModel = repository.save(groupModel);

                foundUser.getGroups().add(groupModel);
                userRepository.save(foundUser);

                GroupDTO response = mapper.entityToDTO(groupModel);
                log.info("{}.createGroup() >> group created :: {}", CLASS_PATH, response);
                return response;
            } catch (DataIntegrityViolationException e) {
                if (!isGroupCodeCollision(e)) {
                    throw new DatabaseException("group-saving-error", "Unable to save new group.");
                }

                log.warn("{}.createGroupModel() >> Group code collision on attempt {}", CLASS_PATH, attempt);
            }
        }

        throw new DatabaseException("group-code-not-generated", "Unable to generate a unique group  after " + MAX_CODE_GENERATION_ATTEMPTS + " attempts.");
    }

    private boolean isGroupCodeCollision(DataIntegrityViolationException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof PSQLException psqlException) {
                ServerErrorMessage error = psqlException.getServerErrorMessage();

                return error != null && "GRP_GROUPS_GROUP_CODE_KEY".equals(error.getConstraint());
            }

            cause = cause.getCause();
        }

        return false;
    }

}
