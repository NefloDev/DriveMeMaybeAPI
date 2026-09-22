package neflo.dev.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.group.api.GroupDTO;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.mapper.GroupMapper;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.user.api.UserDTO;
import neflo.dev.user.api.UserPreferencesFilters;
import neflo.dev.user.api.UserResponseDTO;
import neflo.dev.user.domain.LanguagesModel;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.mapper.UserMapper;
import neflo.dev.user.infrastructure.persistence.LanguagesRepository;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {

    private static final String CLASS_PATH = "DriveMeMaybeAPI.UserService";

    private final UserRepository repository;
    private final GroupRepository groupRepository;
    private final LanguagesRepository langRepository;
    private final UserMapper mapper;
    private final GroupMapper groupMapper;

    public UserResponseDTO getUserDetail(UUID uuid) {
        log.info("{}.getUserDetail() >> uuid :: {}", CLASS_PATH, uuid);

        UserModel repositoryResponse = getUserOrThrow(uuid);

        log.info("{}.getUserDetail() >> user found :: {}", CLASS_PATH, repositoryResponse);
        return mapper.entityToResponseDTO(repositoryResponse);
    }

    public UserPreferencesFilters getUserPreferencesFilters() {
        List<LanguagesModel> languages = langRepository.findAll();

        return new UserPreferencesFilters(
                languages.stream().map(mapper::languageEntityToDTO).sorted(Comparator.comparing(UserPreferencesFilters.LanguageDTO::code)).toList()
        );
    }

    private @NonNull UserModel getUserOrThrow(UUID uuid) {
        return repository.findById(uuid)
                .orElseThrow(() -> new NoEntitiesFoundException("user-not-found", "User not found."));
    }

    public UserResponseDTO updateUser(UUID uuid, UserDTO dto) {
        log.info("{}.updateUser() >> uuid :: {} -- dto :: {}", CLASS_PATH, uuid, dto);

        UserModel repositoryResponse = getUserOrThrow(uuid);

        LanguagesModel langByPref = langRepository.findByCode(Objects.requireNonNullElse(dto.preferences().preferredLang(), "EN"));
        mapper.updateEntity(repositoryResponse, dto);
        repositoryResponse.setPrefLang(langByPref);

        boolean hasUpdatedPassword = false;
        if (dto.password().isPresent()) {
            String password = dto.password().get();
            if (!password.equals(repositoryResponse.getPassword())) {
                hasUpdatedPassword = true;
                repositoryResponse.setPassword(password);
            }
        }

        boolean hasUpdatedPfp = false;
        if (dto.pfp().isPresent()) {
            String pfpB64 = dto.pfp().get();
            if (!pfpB64.equals(repositoryResponse.getPfp())) {
                hasUpdatedPfp = true;
                repositoryResponse.setPfp(pfpB64);
            }
        }

        repositoryResponse = repository.save(repositoryResponse);

        if (!repositoryResponse.equalsDto(dto) && !hasUpdatedPassword && !hasUpdatedPfp) {
            throw new DatabaseException("user-not-updated", "User couldn't be updated at the moment, try again later.");
        }

        log.info("{}.updateUser() >> user updated successfully", CLASS_PATH);
        return mapper.entityToResponseDTO(repositoryResponse);
    }

    public void deleteUser(UUID uuid) {
        log.info("{}.deleteUser() >> uuid :: {}", CLASS_PATH, uuid);

        repository.deleteById(uuid);

        if (repository.findById(uuid).isPresent()) {
            throw new DatabaseException("user-not-deleted", "User couldn't be deleted at the moment, try again later.");
        }
        log.info("{}.deleteUser() >> user deleted successfully", CLASS_PATH);
    }

    public List<GroupDTO> getUserGroups(UUID uuid) {
        log.info("{}.getUserGroups() >> uuid :: {}", CLASS_PATH, uuid);

        UserModel repositoryResponse = getUserOrThrow(uuid);
        log.info("{}.getUserGroups() >> user found", CLASS_PATH);

        List<GroupDTO> userGroups = repositoryResponse.getGroups().stream().map(groupMapper::entityToDTO).toList();
        log.info("{}.getUserGroups() >> userGroups :: {}", CLASS_PATH, userGroups.stream().map(GroupDTO::name).toList());

        return userGroups;
    }

    public GroupDTO joinGroup(UUID uuid, String groupCode) {
        log.info("{}.joinGroup() >> uuid :: {} -- groupCode :: {}", CLASS_PATH, uuid, groupCode);

        UserModel repositoryResponse = getUserOrThrow(uuid);
        log.info("{}.joinGroup() >> user found", CLASS_PATH);

        GroupModel groupResponse = groupRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new NoEntitiesFoundException("group-not-found", "Group not found."));
        log.info("{}.joinGroup() >> group found", CLASS_PATH);

        boolean added = repositoryResponse.getGroups().add(groupResponse);

        if (!added) {
            throw new DatabaseException("already-member", "User is already a member of this group.");
        }

        repository.save(repositoryResponse);

        return groupMapper.entityToDTO(groupResponse);
    }

}
