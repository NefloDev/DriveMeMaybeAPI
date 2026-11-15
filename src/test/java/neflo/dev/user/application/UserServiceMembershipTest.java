package neflo.dev.user.application;

import neflo.dev.group.api.GroupDTO;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.group.infrastructure.mapper.GroupMapper;
import neflo.dev.group.infrastructure.persistence.GroupRepository;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.exception.NoEntitiesFoundException;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.mapper.UserMapper;
import neflo.dev.user.infrastructure.persistence.LanguagesRepository;
import neflo.dev.user.infrastructure.persistence.UserRepository;
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

@ExtendWith(MockitoExtension.class)
class UserServiceMembershipTest {

    @Mock
    UserRepository repository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    LanguagesRepository langRepository;

    @Mock
    UserMapper mapper;

    @Mock
    GroupMapper groupMapper;

    @InjectMocks
    UserService underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static UserModel persistedUser() {
        UserModel user = UserModel.builder()
                .id(USER_ID)
                .email("ana@example.com")
                .password("encoded-old")
                .name("Ana")
                .nickname("ana")
                .build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    @Test
    void should_joinGroup_when_notAlreadyMember() {
        UserModel user = persistedUser();
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>());
        group.setTrips(new HashSet<>());
        GroupDTO expected = new GroupDTO(GROUP_ID, "Riders", "ABCD-1234", 1, 0, Optional.empty());

        given(repository.findById(USER_ID)).willReturn(Optional.of(user));
        given(groupRepository.findByGroupCode("ABCD-1234")).willReturn(Optional.of(group));
        given(groupMapper.entityToDTO(group)).willReturn(expected);

        GroupDTO result = underTest.joinGroup(USER_ID, "ABCD-1234");

        assertThat(result).isEqualTo(expected);
        assertThat(user.getGroups()).contains(group);

        then(repository).should().save(user);
    }

    @Test
    void should_throwDatabase_when_alreadyMember() {
        UserModel user = persistedUser();
        GroupModel group = GroupModel.builder().id(GROUP_ID).name("Riders").groupCode("ABCD-1234").build();
        group.setMembers(new HashSet<>());
        group.setTrips(new HashSet<>());
        user.setGroups(new HashSet<>(Set.of(group)));

        given(repository.findById(USER_ID)).willReturn(Optional.of(user));
        given(groupRepository.findByGroupCode("ABCD-1234")).willReturn(Optional.of(group));

        assertThatThrownBy(() -> underTest.joinGroup(USER_ID, "ABCD-1234"))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("already a member");

        then(repository).should(never()).save(any());
    }

    @Test
    void should_throwNotFound_when_groupCodeUnknown() {
        given(repository.findById(USER_ID)).willReturn(Optional.of(persistedUser()));
        given(groupRepository.findByGroupCode("NOPE-0000")).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.joinGroup(USER_ID, "NOPE-0000"))
                .isInstanceOf(NoEntitiesFoundException.class);
    }

    @Test
    void should_throwNotFound_when_userMissingOnJoin() {
        given(repository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.joinGroup(USER_ID, "ABCD-1234"))
                .isInstanceOf(NoEntitiesFoundException.class);

        then(groupRepository).shouldHaveNoInteractions();
    }
}
