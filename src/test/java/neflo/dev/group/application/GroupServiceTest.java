package neflo.dev.group.application;

import neflo.dev.group.api.GroupDTO;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.mapper.GroupMapper;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.shared.exception.ValidationException;
import neflo.dev.shared.persistence.JDBCRepository;
import neflo.dev.trip.infrastructure.mapper.TripMapper;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.mapper.UserMapper;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

// Note: 7 collaborators exceed the max-3-mocks guideline; GroupService is doing
// retrieval + mutation + insights orchestration and is a refactor candidate (SRP).
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

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
        UserModel user = UserModel.builder().id(id).email(id + "@example.com").name("Member").nickname("member").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    private static GroupModel groupWith(UserModel... members) {
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").pfp("pfp").build();
        group.setMembers(new HashSet<>(Set.of(members)));
        group.setTrips(new HashSet<>());
        return group;
    }

    private static GroupDTO dto(String name, Optional<String> pfp) {
        return new GroupDTO(GROUP_ID, name, "ABCD-1234", 1, 0, pfp);
    }

    @Nested
    class Detail {

        @Test
        void should_returnDetail_when_member() {
            UserModel user = member(USER_ID);
            GroupModel group = groupWith(user);
            GroupDTO expected = dto("Riders", Optional.of("pfp"));

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
            given(mapper.entityToDTO(group)).willReturn(expected);

            assertThat(underTest.getGroupDetail(USER_ID, GROUP_ID)).isEqualTo(expected);
        }

        @Test
        void should_throwNotFound_when_groupMissing() {
            given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.getGroupDetail(USER_ID, GROUP_ID))
                    .isInstanceOf(NoEntitiesFoundException.class)
                    .hasMessageContaining("Group not found");
        }

        @Test
        void should_throwValidation_when_nonMember() {
            GroupModel group = groupWith(member(OTHER_ID));

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));

            assertThatThrownBy(() -> underTest.getGroupDetail(USER_ID, GROUP_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Non members");
        }
    }

    @Nested
    class Update {

        @Test
        void should_updatePfp_when_pfpChanged() {
            UserModel user = member(USER_ID);
            GroupModel group = groupWith(user);
            group.setPfp("old");
            GroupDTO input = dto("Riders", Optional.of("new-pfp"));
            GroupDTO expected = dto("Riders", Optional.of("new-pfp"));

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
            given(repository.save(group)).willReturn(group);
            given(mapper.entityToDTO(group)).willReturn(expected);

            GroupDTO result = underTest.updateGroup(USER_ID, GROUP_ID, input);

            assertThat(result).isEqualTo(expected);
            assertThat(group.getPfp()).isEqualTo("new-pfp");
        }

        @Test
        void should_keepPfp_when_sameValue() {
            UserModel user = member(USER_ID);
            GroupModel group = groupWith(user);
            group.setPfp("same");
            GroupDTO input = dto("Riders", Optional.of("same"));

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
            given(repository.save(group)).willReturn(group);
            given(mapper.entityToDTO(group)).willReturn(input);

            underTest.updateGroup(USER_ID, GROUP_ID, input);

            assertThat(group.getPfp()).isEqualTo("same");
        }

        @Test
        void should_throwDatabase_when_nameChangedWithoutPfpUpdate() {
            UserModel user = member(USER_ID);
            GroupModel group = groupWith(user);
            group.setName("Old");
            group.setPfp("pfp");
            GroupDTO input = dto("Completely different", Optional.empty());

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
            given(repository.save(group)).willReturn(group);

            assertThatThrownBy(() -> underTest.updateGroup(USER_ID, GROUP_ID, input))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("couldn't be updated");
        }

        @Test
        void should_throwNotFound_when_updatingMissingGroup() {
            given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.updateGroup(USER_ID, GROUP_ID, dto("Riders", Optional.empty())))
                    .isInstanceOf(NoEntitiesFoundException.class);

            then(repository).should(never()).save(any());
        }
    }

    @Nested
    class Leave {

        @Test
        void should_deleteGroup_when_lastMemberLeaves() {
            UserModel user = member(USER_ID);
            GroupModel group = groupWith(user);
            user.setGroups(new HashSet<>(Set.of(group)));

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));

            underTest.leaveGroup(USER_ID, GROUP_ID);

            then(repository).should().delete(group);
            then(repository).should(never()).save(any());
        }

        @Test
        void should_removeMember_when_otherMembersRemain() {
            UserModel user = member(USER_ID);
            UserModel other = member(OTHER_ID);
            GroupModel group = groupWith(user, other);

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));
            given(repository.save(group)).willReturn(group);

            underTest.leaveGroup(USER_ID, GROUP_ID);

            assertThat(group.getMembers()).doesNotContain(user);
            then(repository).should().save(group);
        }

        @Test
        void should_doNothing_when_nonMemberLeaves() {
            GroupModel group = groupWith(member(OTHER_ID));

            given(repository.findById(GROUP_ID)).willReturn(Optional.of(group));

            underTest.leaveGroup(USER_ID, GROUP_ID);

            then(repository).should(never()).save(any());
            then(repository).should(never()).delete(any());
        }

        @Test
        void should_throwNotFound_when_leavingMissingGroup() {
            given(repository.findById(GROUP_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.leaveGroup(USER_ID, GROUP_ID))
                    .isInstanceOf(NoEntitiesFoundException.class);
        }
    }
}
