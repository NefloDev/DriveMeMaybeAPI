package neflo.dev.auth.application;

import neflo.dev.auth.api.LoginResponse;
import neflo.dev.shared.exception.AuthenticationException;
import neflo.dev.shared.exception.ValidationException;
import neflo.dev.shared.security.JwtService;
import neflo.dev.user.api.UserDTO;
import neflo.dev.user.api.UserLoginDTO;
import neflo.dev.user.api.UserPreferences;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

// Note: 4 collaborators exceed max-3-mocks; AuthenticationService couples registration,
// authentication and token issuance and is a refactor candidate.
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthenticationService underTest;

    private static UserDTO signupDto(Optional<String> password, Optional<String> pfp) {
        return new UserDTO("ana@example.com", password, "Ana", "ana", pfp,
                new UserPreferences(false, "EN"));
    }

    private static UserModel persistedUser() {
        UserModel user = UserModel.builder().id(java.util.UUID.randomUUID())
                .email("ana@example.com").name("Ana").nickname("ana").password("encoded").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    @Test
    void should_signup_when_newUserWithoutPfp() {
        UserDTO dto = signupDto(Optional.of("secret"), Optional.empty());
        UserModel saved = persistedUser();

        given(userRepository.existsByEmail("ana@example.com")).willReturn(false);
        given(passwordEncoder.encode("secret")).willReturn("encoded");
        given(userRepository.save(any(UserModel.class))).willReturn(saved);
        given(jwtService.generateToken(saved)).willReturn("jwt");
        given(jwtService.getExpirationTime()).willReturn(3600000L);

        LoginResponse result = underTest.signup(dto);

        assertThat(result.token()).isEqualTo("jwt");
        assertThat(result.expiresOn()).isEqualTo(3600000L);
    }

    @Test
    void should_signup_when_pfpPresent() {
        UserDTO dto = signupDto(Optional.of("secret"), Optional.of("pfp-b64"));
        UserModel saved = persistedUser();

        given(userRepository.existsByEmail("ana@example.com")).willReturn(false);
        given(passwordEncoder.encode("secret")).willReturn("encoded");
        given(userRepository.save(any(UserModel.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtService.generateToken(any(UserModel.class))).willReturn("jwt");
        given(jwtService.getExpirationTime()).willReturn(1L);

        LoginResponse result = underTest.signup(dto);

        assertThat(result.token()).isEqualTo("jwt");
    }

    @Test
    void should_throwAuthentication_when_emailTaken() {
        given(userRepository.existsByEmail("ana@example.com")).willReturn(true);

        assertThatThrownBy(() -> underTest.signup(signupDto(Optional.of("secret"), Optional.empty())))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("already in use");

        then(userRepository).should(never()).save(any());
    }

    @Test
    void should_throwValidation_when_passwordEmpty() {
        given(userRepository.existsByEmail("ana@example.com")).willReturn(false);

        assertThatThrownBy(() -> underTest.signup(signupDto(Optional.empty(), Optional.empty())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password field is empty");
    }

    @Test
    void should_authenticate_when_credentialsValid() {
        UserLoginDTO login = new UserLoginDTO("ana@example.com", "secret");
        UserModel user = persistedUser();

        given(userRepository.findByEmail("ana@example.com")).willReturn(Optional.of(user));
        given(jwtService.generateToken(user)).willReturn("jwt");
        given(jwtService.getExpirationTime()).willReturn(5L);

        LoginResponse result = underTest.authenticate(login);

        assertThat(result.token()).isEqualTo("jwt");
        then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void should_throwAuthentication_when_managerRejects() {
        UserLoginDTO login = new UserLoginDTO("ana@example.com", "wrong");

        given(authenticationManager.authenticate(any())).willThrow(new RuntimeException("bad credentials"));

        assertThatThrownBy(() -> underTest.authenticate(login))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("authenticating");
    }

    @Test
    void should_throwNoSuchElement_when_userVanishedAfterAuth() {
        UserLoginDTO login = new UserLoginDTO("ghost@example.com", "secret");

        given(userRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.authenticate(login))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void should_refresh_when_userGiven() {
        UserModel user = persistedUser();

        given(jwtService.generateToken(user)).willReturn("refreshed");
        given(jwtService.getExpirationTime()).willReturn(9L);

        LoginResponse result = underTest.refreshToken(user);

        assertThat(result.token()).isEqualTo("refreshed");
        assertThat(result.expiresOn()).isEqualTo(9L);
    }

    @Test
    void should_returnTrue_when_emailExists() {
        given(userRepository.existsByEmail("ana@example.com")).willReturn(true);

        assertThat(underTest.isUserRegistered(signupDto(Optional.of("x"), Optional.empty()))).isTrue();
    }

    @Test
    void should_returnFalse_when_emailUnknown() {
        given(userRepository.existsByEmail("ana@example.com")).willReturn(false);

        assertThat(underTest.isUserRegistered(signupDto(Optional.of("x"), Optional.empty()))).isFalse();
    }
}
