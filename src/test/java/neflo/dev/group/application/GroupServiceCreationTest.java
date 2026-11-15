package neflo.dev.group.application;

import neflo.dev.group.api.GroupDTO;
import neflo.dev.group.api.GroupRequestDTO;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.mapper.GroupMapper;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.shared.persistence.JDBCRepository;
import neflo.dev.trip.infrastructure.mapper.TripMapper;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.mapper.UserMapper;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GroupServiceCreationTest {

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

    private static UserModel persistedUser() {
        UserModel user = UserModel.builder().id(USER_ID).email("ana@example.com").name("Ana").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    private static DataIntegrityViolationException collision() {
        ServerErrorMessage serverError = mock(ServerErrorMessage.class);
        given(serverError.getConstraint()).willReturn("GRP_GROUPS_GROUP_CODE_KEY");
        PSQLException psql = mock(PSQLException.class);
        given(psql.getServerErrorMessage()).willReturn(serverError);
        return new DataIntegrityViolationException("duplicate group_code", psql);
    }

    @Test
    void should_createGroup_when_noPfp() {
        UserModel user = persistedUser();
        GroupRequestDTO request = new GroupRequestDTO("Riders", Optional.empty());
        GroupModel saved = GroupModel.builder().id(UUID.randomUUID()).name("Riders").groupCode("ABCD-1234").build();
        saved.setMembers(new HashSet<>());
        saved.setTrips(new HashSet<>());
        GroupDTO expected = new GroupDTO(saved.getId(), "Riders", "ABCD-1234", 1, 0, Optional.empty());

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(codeGenerator.generate()).willReturn("ABCD-1234");
        given(repository.save(any(GroupModel.class))).willReturn(saved);
        given(mapper.entityToDTO(saved)).willReturn(expected);

        GroupDTO result = underTest.createGroup(USER_ID, request);

        assertThat(result).isEqualTo(expected);
        then(userRepository).should().save(user);
    }

    @Test
    void should_createGroup_when_pfpPresent() {
        UserModel user = persistedUser();
        GroupRequestDTO request = new GroupRequestDTO("Riders", Optional.of("pfp-b64"));
        GroupModel saved = GroupModel.builder().id(UUID.randomUUID()).name("Riders").groupCode("ABCD-1234").pfp("pfp-b64").build();
        saved.setMembers(new HashSet<>());
        saved.setTrips(new HashSet<>());
        GroupDTO expected = new GroupDTO(saved.getId(), "Riders", "ABCD-1234", 1, 0, Optional.of("pfp-b64"));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(codeGenerator.generate()).willReturn("ABCD-1234");
        given(repository.save(any(GroupModel.class))).willReturn(saved);
        given(mapper.entityToDTO(saved)).willReturn(expected);

        assertThat(underTest.createGroup(USER_ID, request)).isEqualTo(expected);
    }

    @Test
    void should_throwNotFound_when_userMissing() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.createGroup(USER_ID, new GroupRequestDTO("Riders", Optional.empty())))
                .isInstanceOf(NoEntitiesFoundException.class);

        then(repository).shouldHaveNoInteractions();
    }

    @Test
    void should_throwDatabase_when_nonCollisionIntegrityError() {
        UserModel user = persistedUser();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(codeGenerator.generate()).willReturn("ABCD-1234");
        given(repository.save(any(GroupModel.class)))
                .willThrow(new DataIntegrityViolationException("other constraint", new RuntimeException("other")));

        assertThatThrownBy(() -> underTest.createGroup(USER_ID, new GroupRequestDTO("Riders", Optional.empty())))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("Unable to save new group");
    }

    @Test
    void should_throwDatabase_when_psqlWithoutServerMessage() {
        UserModel user = persistedUser();
        PSQLException psqlWithoutMessage = mock(PSQLException.class);
        given(psqlWithoutMessage.getServerErrorMessage()).willReturn(null);
        DataIntegrityViolationException noMessage = new DataIntegrityViolationException("dup", psqlWithoutMessage);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(codeGenerator.generate()).willReturn("ABCD-1234");
        given(repository.save(any(GroupModel.class))).willThrow(noMessage);

        assertThatThrownBy(() -> underTest.createGroup(USER_ID, new GroupRequestDTO("Riders", Optional.empty())))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("Unable to save new group");
    }

    @Test
    void should_retry_when_codeCollidesOnce() {
        UserModel user = persistedUser();
        GroupModel saved = GroupModel.builder().id(UUID.randomUUID()).name("Riders").groupCode("WXYZ-9876").build();
        saved.setMembers(new HashSet<>());
        saved.setTrips(new HashSet<>());
        GroupDTO expected = new GroupDTO(saved.getId(), "Riders", "WXYZ-9876", 1, 0, Optional.empty());
        DataIntegrityViolationException firstAttemptCollision = collision();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(codeGenerator.generate()).willReturn("ABCD-1234", "WXYZ-9876");
        given(repository.save(any(GroupModel.class))).willThrow(firstAttemptCollision).willReturn(saved);
        given(mapper.entityToDTO(saved)).willReturn(expected);

        assertThat(underTest.createGroup(USER_ID, new GroupRequestDTO("Riders", Optional.empty())))
                .isEqualTo(expected);
    }

    @Test
    void should_throwDatabase_when_collisionsExhausted() {
        UserModel user = persistedUser();
        DataIntegrityViolationException persistentCollision = collision();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(codeGenerator.generate()).willReturn("ABCD-1234");
        given(repository.save(any(GroupModel.class))).willThrow(persistentCollision);

        assertThatThrownBy(() -> underTest.createGroup(USER_ID, new GroupRequestDTO("Riders", Optional.empty())))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("Unable to generate a unique group");

        then(codeGenerator).should(times(5)).generate();
    }
}
